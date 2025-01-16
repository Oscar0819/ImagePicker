package com.milet0819.imagepicker

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.milet0819.imagepicker.databinding.ActivityMainBinding
import com.milet0819.notificationtest.common.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val requestPermissons = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        logger(results)
        results.keys.forEach {
            val key = it
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btImagePicker.setOnClickListener {
            /**
             * 선택 가능한 미디어 유형을 권한별로 필터링
             * 선택 대화상자는 요청된 권한 유형에 따라 다릅니다.
             *
             * READ_MEDIA_IMAGES만 요청하면 선택 가능한 이미지만 표시됩니다.
             * READ_MEDIA_VIDEO만 요청하면 선택 가능한 동영상만 표시됩니다.
             * READ_MEDIA_IMAGES 및 READ_MEDIA_VIDEO를 모두 요청하면 전체 사진 라이브러리가 선택 가능한 것으로 표시됩니다.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                requestPermissons.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissons.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
            } else {
                requestPermissons.launch(arrayOf(READ_EXTERNAL_STORAGE))
            }
        }
    }

}