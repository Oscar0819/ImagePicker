package com.milet0819.imagepicker

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.milet0819.imagepicker.databinding.ActivityImagePickerBinding
import com.milet0819.imagepicker.utils.toPx
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
}