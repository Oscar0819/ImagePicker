package com.milet0819.imagepicker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.milet0819.imagepicker.databinding.PermissionBottomSheetDialogBinding

class PermissionBottomSheetDialog(context: Context, val listener: PermissionBottomSheetActions): BottomSheetDialog(context) {

    interface PermissionBottomSheetActions {
        fun onRequestMorePhotos()
        fun onRequestAllPhotosPermission()
    }

    private val binding: PermissionBottomSheetDialogBinding by lazy {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        PermissionBottomSheetDialogBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initListener()
    }

    private fun initListener() = with(binding) {
        tvPermissionBottomSheetMorePhotoSelect.setOnClickListener {
            listener.onRequestMorePhotos()
            dismiss()
        }

        tvPermissionBottomSheetAccessGrantAllPhoto.setOnClickListener {
            listener.onRequestAllPhotosPermission()
            dismiss()
        }
    }
}