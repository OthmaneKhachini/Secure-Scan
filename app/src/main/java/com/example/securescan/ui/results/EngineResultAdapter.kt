package com.example.securescan.ui.results

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.securescan.data.model.EngineResult
import com.example.securescan.databinding.ItemEngineResultBinding

class EngineResultAdapter(
    private var results: List<EngineResult>
) : RecyclerView.Adapter<EngineResultAdapter.ViewHolder>() {

    private var filteredResults = results

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEngineResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredResults[position])
    }

    override fun getItemCount() = filteredResults.size

    fun updateResults(newResults: List<EngineResult>) {
        results = newResults
        filteredResults = newResults
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredResults = if (query.isEmpty()) {
            results
        } else {
            results.filter {
                it.engineName.contains(query, ignoreCase = true) ||
                        (it.result ?: "").contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemEngineResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: EngineResult) {
            binding.tvEngineName.text = result.engineName
            binding.tvResult.text = result.result ?: "undetected"
            binding.tvCategory.text = result.category
            binding.tvMethod.text = result.method
            
            // Version Info
            if (!result.engineVersion.isNullOrBlank()) {
                binding.llVersionInfo.visibility = View.VISIBLE
                binding.tvVersion.text = result.engineVersion
            } else {
                binding.llVersionInfo.visibility = View.GONE
            }

            // Update Info
            if (!result.engineUpdate.isNullOrBlank()) {
                binding.llUpdateInfo.visibility = View.VISIBLE
                binding.tvUpdateDate.text = result.engineUpdate
            } else {
                binding.llUpdateInfo.visibility = View.GONE
            }
            
            val color = when(result.category.lowercase()) {
                "malicious" -> Color.parseColor("#FF3B30")
                "suspicious" -> Color.parseColor("#FFCC00")
                "harmless" -> Color.parseColor("#4CD964")
                else -> Color.parseColor("#8E8E93")
            }
            binding.tvResult.setTextColor(color)
        }
    }
}