package com.example.agricurify.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agricurify.data.database.HistoryEntity
import com.example.agricurify.databinding.ItemRiwayatPenyakitBinding

class HistoryAdapter(private val onItemClick: (HistoryEntity) -> Unit) :
    ListAdapter<HistoryEntity, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemRiwayatPenyakitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemRiwayatPenyakitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(history: HistoryEntity) {
            binding.cardTitle.text = history.diseaseName
            val confidencePercentage = history.confidence
            binding.textView.text = "Akurasi: %.2f %%".format(confidencePercentage)
            Glide.with(itemView.context).load(history.imageUri).into(binding.imgItemPhoto)

            itemView.setOnClickListener {
                onItemClick(history)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HistoryEntity>() {
            override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) =
                oldItem == newItem
        }
    }
}
