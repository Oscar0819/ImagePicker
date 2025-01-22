package com.milet0819.imagepicker

import android.Manifest
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.milet0819.imagepicker.databinding.ActivityImagePickerBinding
import com.milet0819.imagepicker.utils.isGranted
import com.milet0819.imagepicker.utils.showListOptionAlertDialog
import com.milet0819.notificationtest.common.utils.logger
import com.milet0819.notificationtest.common.utils.registerForActivityResult
import com.milet0819.notificationtest.common.utils.requestPermission
import com.milet0819.notificationtest.common.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagePickerActivity : AppCompatActivity() {

    val binding: ActivityImagePickerBinding by lazy {
        ActivityImagePickerBinding.inflate(layoutInflater)
    }

    val mMediaAdapter by lazy {
        MediaAdapter(object : MediaAdapter.CameraAction {
            override fun onRequestCamera(position: Int) {
                // 재활용으로 인한 이벤트 방지 로직
                if (position != 0) {
                    logger("Is not position 0")
                    return
                }

                if (isGranted(this@ImagePickerActivity, Manifest.permission.CAMERA)) {
                    showCameraOptionsAlert()
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
        })
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        logger(results)
    }

    private val requestCameraPermission = requestPermission { result ->
        logger("CameraPermission : $result")

        if (result) {
            showCameraOptionsAlert()
        } else {
            toast(getString(R.string.permission_rationale, "카메라"))
        }
    }

    private val requestCamera = registerForActivityResult { activityResult ->
        logger("picture result=$activityResult")

        when (activityResult.resultCode) {
            RESULT_OK -> {
                logger("${activityResult.data}")
            }

            RESULT_CANCELED -> {
                logger("canceled")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        setSupportActionBar(binding.tbImagePicker)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initListener()

    }

    override fun onResume() {
        super.onResume()

        initPermissionManageLayout()

        lifecycleScope.launch {
            val images = getImages(contentResolver)
            val spanCount = 3
            val space = 4
            val includeEdge = false
            binding.rvImagePicker.apply {
                addItemDecoration(GridSpaceItemDecoration(spanCount, space, includeEdge))
                adapter = mMediaAdapter
            }

            mMediaAdapter.submitList(images)
        }
    }

    private fun initPermissionManageLayout() = with(binding) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return@with
        }

        if (isGranted(this@ImagePickerActivity, READ_MEDIA_VISUAL_USER_SELECTED) &&
            !isGranted(this@ImagePickerActivity, READ_MEDIA_IMAGES) &&
            !isGranted(this@ImagePickerActivity, READ_MEDIA_VIDEO)
            ) {
            clImagePickerPermissionManage.visibility = View.VISIBLE
        } else {
            clImagePickerPermissionManage.visibility = View.GONE
        }

    }

    // Run the querying logic in a coroutine outside of the main thread to keep the app responsive.
    // Keep in mind that this code snippet is querying only images of the shared storage.
    // TODO ADD getVideo & All
    suspend fun getImages(contentResolver: ContentResolver): List<Media?> = withContext(Dispatchers.IO) {
        val projecttion = arrayOf(
            Images.Media._ID,
            Images.Media.DISPLAY_NAME,
            Images.Media.SIZE,
            Images.Media.MIME_TYPE
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query all the device storage volumes instead of the primary only
            Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            Images.Media.EXTERNAL_CONTENT_URI
        }

        val images = mutableListOf<Media?>()

        images.add(null)

        contentResolver.query(
            collectionUri,
            projecttion,
            null,
            null,
            "${Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val image = Media(uri, name, size, mimeType)

                if (image.size == 0L) {
                    logger("Empty image")
                    continue
                }

                images.add(image)
            }
        }

        return@withContext images
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initListener() {
        binding.tvImagePickerPermissionAccessManage.setOnClickListener {
            val permissionBottomSheetActions = object : PermissionBottomSheetDialog.PermissionBottomSheetActions {
                override fun onRequestMorePhotos() {
                    logger("onRequestMorePhotos")

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        return
                    }

                    val permissions = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)
                    requestPermissions.launch(permissions)
                }

                override fun onRequestAllPhotosPermission() {
                    logger("onRequestAllPhotosPermission")

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        return
                    }

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }

                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        logger("수행할 수 있는 컴포넌트 없음.")
                    }
                }

            }

            PermissionBottomSheetDialog(this@ImagePickerActivity, permissionBottomSheetActions).show()
        }
    }

    /**
     *  Source: Android Developer Documentation (Camera Intents)
     *  URL: https://developer.android.com/media/camera/camera-intents?hl=ko
     */
    private fun showCameraOptionsAlert() {
        showListOptionAlertDialog("촬영", arrayOf("사진 촬영", "동영상 촬영")) { index ->
            when (index) {
                0 -> {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                    try {
                        requestCamera.launch(takePictureIntent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                        toast(R.string.not_found_camera)
                    }
                }
                1 -> {
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                        takeVideoIntent.resolveActivity(packageManager)?.also {
                            requestCamera.launch(takeVideoIntent)
                        } ?: run {
                            //display error state to the user
                            toast(R.string.not_found_camera)
                        }
                    }
                }
            }
        }
    }
}