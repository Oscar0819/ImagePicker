package com.milet0819.imagepicker

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.milet0819.imagepicker.databinding.ActivityImagePickerBinding
import com.milet0819.imagepicker.utils.PermissionUtils
import com.milet0819.imagepicker.utils.toPx
import com.milet0819.notificationtest.common.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagePickerActivity : AppCompatActivity() {

    val binding: ActivityImagePickerBinding by lazy {
        ActivityImagePickerBinding.inflate(layoutInflater)
    }

    val mMediaAdapter by lazy {
        MediaAdapter()
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        logger(results)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        initPermissionManageLayout()

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

        lifecycleScope.launch {
            val images = getImages(contentResolver)
            val spanCount = 3
            val space = 4.toPx(this@ImagePickerActivity)
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

        if (PermissionUtils.isGranted(this@ImagePickerActivity, READ_MEDIA_VISUAL_USER_SELECTED) &&
            !PermissionUtils.isGranted(this@ImagePickerActivity, READ_MEDIA_IMAGES) &&
            !PermissionUtils.isGranted(this@ImagePickerActivity, READ_MEDIA_VIDEO)
            ) {
            clImagePickerPermissionManage.visibility = View.VISIBLE
        }

    }

    // Run the querying logic in a coroutine outside of the main thread to keep the app responsive.
    // Keep in mind that this code snippet is querying only images of the shared storage.
    suspend fun getImages(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
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

        val images = mutableListOf<Media>()

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
}