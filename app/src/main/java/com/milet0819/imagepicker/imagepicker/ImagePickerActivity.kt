package com.milet0819.imagepicker.imagepicker

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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.milet0819.imagepicker.AppController
import com.milet0819.imagepicker.PermissionBottomSheetDialog
import com.milet0819.imagepicker.R
import com.milet0819.imagepicker.databinding.ActivityImagePickerBinding
import com.milet0819.imagepicker.utils.isGranted
import com.milet0819.imagepicker.utils.showListOptionAlertDialog
import com.milet0819.notificationtest.common.utils.captureVideo
import com.milet0819.notificationtest.common.utils.logger
import com.milet0819.notificationtest.common.utils.requestPermission
import com.milet0819.notificationtest.common.utils.takeCamera
import com.milet0819.notificationtest.common.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class ImagePickerActivity : AppCompatActivity() {

    private val binding: ActivityImagePickerBinding by lazy {
        ActivityImagePickerBinding.inflate(layoutInflater)
    }

    private lateinit var mImageUri: Uri
    private lateinit var mVideoUri: Uri

    private var mCurrentCategory = MediaCategory.ALL

    private val mMediaAdapter by lazy {
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

    private val takePicture = takeCamera { result ->
        if (result) {
            logger("Success")
            val resultIntent = Intent().apply {
                putExtra(MEDIA_URI, mImageUri.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            logger("Cancel or can't take a URI")
        }
    }

    private val captureVideo = captureVideo { result ->
        if (result) {
            logger("Success")
            val resultIntent = Intent().apply {
                putExtra(MEDIA_URI, mVideoUri.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            logger("Cancel or can't take a URI")
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
            it.setDisplayShowTitleEnabled(false)
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

        setMedia(mCurrentCategory)
    }

    private fun setMedia(category: MediaCategory) =lifecycleScope.launch {
        val spanCount = 3
        val space = 4
        val includeEdge = false
        binding.rvImagePicker.apply {
            addItemDecoration(GridSpaceItemDecoration(spanCount, space, includeEdge))
            adapter = mMediaAdapter
        }

        val mediaList: List<Media?>
        when (category) {
            MediaCategory.ALL -> {
                mediaList = getMediaList(contentResolver)
                binding.tvMenu.text = getString(R.string.menu_all)
            }

            MediaCategory.IMAGE -> {
                mediaList = getImages(contentResolver)
                binding.tvMenu.text = getString(R.string.menu_image)
            }

            MediaCategory.VIDEO -> {
                mediaList = getVideos(contentResolver)
                binding.tvMenu.text = getString(R.string.menu_video)
            }
        }

        mMediaAdapter.submitList(mediaList)
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
    private suspend fun getImages(contentResolver: ContentResolver): List<Media?> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
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
            projection,
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

                val image = Media(
                    uri = uri,
                    name = name,
                    size = size,
                    mimeType = mimeType)

                // TODO 추가적인 처리?
                if (image.size == 0L) {
                    logger("Empty image")
                    continue
                }

                images.add(image)
            }
        }

        return@withContext images
    }

    private suspend fun getVideos(contentResolver: ContentResolver): List<Media?> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query all the device storage volumes instead of the primary only
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val videos = mutableListOf<Media?>()

        videos.add(null)

        contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getInt(durationColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val video = Media(
                    uri = uri,
                    name = name,
                    size = size,
                    duration = duration,
                    mimeType = mimeType)

                // TODO 추가적인 처리?
                if (video.size == 0L) {
                    logger("Empty image")
                    continue
                }

                videos.add(video)
            }
        }

        return@withContext videos
    }

    private suspend fun getMediaList(contentResolver: ContentResolver): List<Media?> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query all the device storage volumes instead of the primary only
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            // TODO 미디어 리스트 가져올 때 Q버전 미만에서 크래시 발생하는 부분 수정 중
            MediaStore.Files.getContentUri("external")
        }

        val mediaList = mutableListOf<Media?>()

        mediaList.add(null)

        contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getInt(durationColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val media = Media(
                    uri = uri,
                    name = name,
                    size = size,
                    duration = duration,
                    mimeType = mimeType)

                // TODO 추가적인 처리?
                if (media.size == 0L) {
                    logger("Empty image")
                    continue
                }

                mediaList.add(media)
            }
        }

        return@withContext mediaList
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
        binding.clMenu.setOnClickListener {
            val popup = PopupMenu(this@ImagePickerActivity, it)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.all -> {
                        mCurrentCategory = MediaCategory.ALL
                        setMedia(mCurrentCategory)
                        true
                    }
                    R.id.image -> {
                        mCurrentCategory = MediaCategory.IMAGE
                        setMedia(mCurrentCategory)
                        true
                    }
                    R.id.video -> {
                        mCurrentCategory = MediaCategory.VIDEO
                        setMedia(mCurrentCategory)
                        true
                    }

                    else -> false
                }
            }
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.image_picker_toolbar_menu, popup.menu)
            popup.show()
        }

        binding.tvImagePickerPermissionAccessManage.setOnClickListener {
            val permissionBottomSheetActions = object :
                PermissionBottomSheetDialog.PermissionBottomSheetActions {
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
            val now = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

            when (index) {
                0 -> {
                    val dirPath = AppController.imagesDirPath
                    val imageDir = File(dirPath)
                    imageDir.mkdirs()

                    val fileName = "img_$now.jpg"
                    val imageFile = File(dirPath, fileName)
                    val imageUri = FileProvider.getUriForFile(this@ImagePickerActivity, "${packageName}.fileprovider", imageFile)
                    mImageUri = imageUri
                    takePicture.launch(imageUri)
                }
                1 -> {
                    val dirPath = AppController.videoDirPath
                    val videoDir = File(dirPath)
                    videoDir.mkdirs()

                    val fileName = "vid_$now.mp4"
                    val videoFile = File(videoDir, fileName)
                    val videoUri = FileProvider.getUriForFile(this@ImagePickerActivity, "${packageName}.fileprovider", videoFile)
                    mVideoUri = videoUri
                    captureVideo.launch(videoUri)
                }
            }
        }
    }

    companion object {
        const val MEDIA_URI = "mediaUri"
    }

    enum class MediaCategory {
        ALL, IMAGE, VIDEO
    }
}