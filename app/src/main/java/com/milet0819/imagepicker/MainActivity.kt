package com.milet0819.imagepicker

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.milet0819.imagepicker.databinding.ActivityMainBinding
import com.milet0819.imagepicker.imagepicker.ImagePickerActivity
import com.milet0819.imagepicker.utils.clearDir
import com.milet0819.imagepicker.utils.isGranted
import com.milet0819.notificationtest.common.utils.buildIntent
import com.milet0819.notificationtest.common.utils.logger
import com.milet0819.notificationtest.common.utils.registerForActivityResult
import com.milet0819.notificationtest.common.utils.toast
import java.io.File

class MainActivity : AppCompatActivity() {

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val imagePickerLauncher = registerForActivityResult { result ->
        if (result.resultCode == RESULT_OK) {
            val mediaUri = result.data?.getStringExtra(ImagePickerActivity.MEDIA_URI)

            if (mediaUri == null) {
                logger("Media URI is null")
                return@registerForActivityResult
            }

             val uri = Uri.parse(mediaUri)

            Glide.with(baseContext).load(uri).into(binding.ivTest)
        }
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        logger(results)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val imagesPerm = results.getValue(READ_MEDIA_IMAGES)
            val videoPerm = results.getValue(READ_MEDIA_VIDEO)
            val selectedPerm = results.getValue(READ_MEDIA_VISUAL_USER_SELECTED)

            // *READ_MEDIA_VISUAL_USER_SELECTED 권한 정리* 케이스 참고
            if (imagesPerm && videoPerm && selectedPerm) {
                imagePickerLauncher.launch(buildIntent<ImagePickerActivity>())
            } else if (selectedPerm) {
                imagePickerLauncher.launch(buildIntent<ImagePickerActivity>())
            } else {
                logger("Denied Permission")
                toast("접근 권한이 없어 해당 기능을 사용할 수 없습니다.") // This sentence was referenced from KakaoTalk.
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagesPerm = results.getValue(READ_MEDIA_IMAGES)
            val videoPerm = results.getValue(READ_MEDIA_VIDEO)

            if (imagesPerm && videoPerm) {
                imagePickerLauncher.launch(buildIntent<ImagePickerActivity>())
            } else {
                logger("Denied Permission")
                toast("접근 권한이 없어 해당 기능을 사용할 수 없습니다.")
            }

        } else {
            val readExternalStoragePerm = results.getValue(READ_EXTERNAL_STORAGE)

            if (readExternalStoragePerm) {
                imagePickerLauncher.launch(buildIntent<ImagePickerActivity>())
            } else {
                logger("Denied Permission")
                toast("접근 권한이 없어 해당 기능을 사용할 수 없습니다.")
            }
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
                if (isGranted(baseContext, READ_MEDIA_VISUAL_USER_SELECTED)) {
                    imagePickerLauncher.launch(buildIntent<ImagePickerActivity>())
                } else {
                    requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
            } else {
                requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        /**
         * onDestroy는 호출이 보장되지 않아서 캐시디렉토리를 이용
         */
        clearDir(File(cacheDir, AppController.IMAGES_DIR))
        clearDir(File(cacheDir, AppController.VIDEOS_DIR))
    }

}