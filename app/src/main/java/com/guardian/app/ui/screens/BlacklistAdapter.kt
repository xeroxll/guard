package com.guardian.app.ui.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardian.app.data.BlacklistedApp
import com.guardian.app.databinding.ItemBlacklistBinding

class BlacklistAdapter(
    private val onDeleteClick: (BlacklistedApp) -> Unit
) : ListAdapter<BlacklistedApp, BlacklistAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlacklistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemBlacklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(app: BlacklistedApp) {
            binding.appName.text = app.name
            binding.packageName.text = app.packageName
            binding.deleteButton.setOnClickListener {
                onDeleteClick(app)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<BlacklistedApp>() {
        override fun areItemsTheSame(oldItem: BlacklistedApp, newItem: BlacklistedApp): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BlacklistedApp, newItem: BlacklistedApp): Boolean {
            return oldItem == newItem
        }
    }
}
