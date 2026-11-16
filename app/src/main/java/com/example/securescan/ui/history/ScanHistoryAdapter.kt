package com.example.securescan.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securescan.R
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.databinding.ItemScanHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class ScanHistoryAdapter(
    private val onItemClick: (ScanEntity) -> Unit,
    private val onItemLongClick: (ScanEntity) -> Unit,
    private val onDeleteClick: (ScanEntity) -> Unit,
    private val onSelectionChanged: () -> Unit
) : ListAdapter<ScanEntity, ScanHistoryAdapter.ViewHolder>(DiffCallback) {

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<ScanEntity>()

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode == enabled) return
        isSelectionMode = enabled
        if (!enabled) selectedItems.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(scan: ScanEntity) {
        if (selectedItems.contains(scan)) {
            selectedItems.remove(scan)
        } else {
            selectedItems.add(scan)
        }
        notifyItemChanged(currentList.indexOf(scan))
        onSelectionChanged()
    }

    fun getSelectedItems(): List<ScanEntity> = selectedItems.toList()

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scan = getItem(position)
        holder.bind(scan)
    }

    inner class ViewHolder(private val binding: ItemScanHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ScanEntity) {
            val context = itemView.context
            binding.tvTarget.text = scan.target
            binding.tvType.text = scan.type
            
            val malicious = scan.maliciousCount
            if (malicious > 0) {
                binding.tvStats.text = context.getString(R.string.history_malicious_detected, malicious)
                binding.tvStats.setTextColor(context.getColor(R.color.cyber_malicious))
                binding.vStatusIndicator.setBackgroundColor(context.getColor(R.color.cyber_malicious))
            } else {
                binding.tvStats.text = context.getString(R.string.history_clean)
                binding.tvStats.setTextColor(context.getColor(R.color.cyber_safe))
                binding.vStatusIndicator.setBackgroundColor(context.getColor(R.color.cyber_safe))
            }
            
            binding.ivIcon.setImageResource(
                if (scan.type == "URL") R.drawable.ic_url else R.drawable.ic_file
            )

            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.tvDate.text = context.getString(R.string.scan_date_format, sdf.format(Date(scan.date)))

            // Selection Mode UI
            binding.cbSelection.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.cbSelection.isChecked = selectedItems.contains(scan)
            binding.btnDeleteItem.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(scan)
                } else {
                    onItemClick(scan)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(scan)
                true
            }

            binding.btnDeleteItem.setOnClickListener {
                onDeleteClick(scan)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScanEntity>() {
        override fun areItemsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanEntity, newItem: ScanEntity): Boolean {
            return oldItem == newItem
        }
    }
}