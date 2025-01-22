package com.milet0819.imagepicker

import android.Manifest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.milet0819.imagepicker.databinding.ItemMediaBinding
import com.milet0819.imagepicker.utils.isGranted
import com.milet0819.notificationtest.common.utils.logger

class MediaAdapter(val cameraAction: CameraAction): ListAdapter<Media, MediaAdapter.MediaViewHolder>(object : DiffUtil.ItemCallback<Media>() {
    /**
     * areItemsTheSame 함수가 먼저 실행이 되고 해당 함수의 결과로 true가 반환됐을 경우에만 areContentsTheSame()이 호출됨.
     * 그렇기 때문에 areItemsTheSame()에는 일반적으로 id 처럼 아이템을 식별할 수 있는 유니크한 값을 비교하고,
     * areContentsTheSame()에는 아이템의 내부 정보가 모두 동일한지 비교함. 그래서 areContentsTheSame 에서는 보통 equals 함수를 활용함.
     */
    //
    override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
        return oldItem == newItem
    }
}) {

    interface CameraAction {
        fun onRequestCamera(position: Int)
    }

    inner class MediaViewHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        val context = binding.root.context

        fun bind(item: Media?) {
            binding.also {
                if (item == null) {
                    it.ivMedia.visibility = View.GONE
                    it.ivCamera.visibility = View.VISIBLE
//                    it.root.setBackgroundColor(Color.GRAY)
                    binding.root.setOnClickListener {
                        cameraAction.onRequestCamera(layoutPosition)
                    }

                } else {
                    it.ivMedia.visibility = View.VISIBLE
                    it.ivCamera.visibility = View.GONE

                    // TODO CHECK Glide options
                    Glide.with(it.ivMedia).load(item.uri).into(it.ivMedia)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder =
        MediaViewHolder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = currentList.size

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}