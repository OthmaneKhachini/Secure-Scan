package com.example.securescan.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securescan.R
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

/**
 * Fragment displaying the consolidated intelligence history.
 * Provides management tools for searching, selecting, and deleting scan records.
 */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: ScanHistoryAdapter
    private var allScansList: List<ScanEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ScanHistoryAdapter(
            onItemClick = { scan ->
                val action = HistoryFragmentDirections
                    .actionHistoryFragmentToResultDetailFragment(scan.scanId, scan.type)
                findNavController().navigate(action)
            },
            onItemLongClick = { scan ->
                enterSelectionMode()
                adapter.toggleSelection(scan)
            },
            onDeleteClick = { scan ->
                showDeleteConfirmation(listOf(scan))
            },
            onSelectionChanged = {
                val count = adapter.getSelectedItems().size
                binding.tvSelectionCount.text = getString(R.string.items_selected_format, count)
                if (count == 0) exitSelectionMode()
            }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    /**
     * Filters the history list in real-time as the user types.
     */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString()?.lowercase() ?: ""
            val filtered = if (query.isEmpty()) {
                allScansList
            } else {
                allScansList.filter { 
                    it.target.lowercase().contains(query) || 
                    it.type.lowercase().contains(query) 
                }
            }
            adapter.submitList(filtered)
        }
    }

    private fun setupClickListeners() {
        binding.btnDeleteAll.setOnClickListener {
            showDeleteConfirmation(null) // Represents "Delete All"
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showDeleteConfirmation(selected)
            }
        }

        binding.btnCloseSelection.setOnClickListener {
            exitSelectionMode()
        }
    }

    private fun enterSelectionMode() {
        binding.cvSelectionBar.visibility = View.VISIBLE
        binding.cvStorageInfo.visibility = View.GONE
        binding.btnDeleteAll.visibility = View.GONE
        adapter.setSelectionMode(true)
    }

    private fun exitSelectionMode() {
        binding.cvSelectionBar.visibility = View.GONE
        binding.cvStorageInfo.visibility = View.VISIBLE
        binding.btnDeleteAll.visibility = View.VISIBLE
        adapter.clearSelection()
    }

    /**
     * Displays a professional confirmation dialog before performing deletion.
     * @param scans The list of entities to delete, or null if purging the entire database.
     */
    private fun showDeleteConfirmation(scans: List<ScanEntity>?) {
        val title = if (scans == null) getString(R.string.delete_all_title) else getString(R.string.delete_selected_title)
        val message = getString(R.string.delete_confirm_message, scans?.size?.toString() ?: "all")
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                if (scans == null) {
                    viewModel.deleteAllScans()
                    Toast.makeText(requireContext(), R.string.logs_cleared, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteScans(scans)
                    Toast.makeText(requireContext(), getString(R.string.items_removed, scans.size), Toast.LENGTH_SHORT).show()
                }
                exitSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        // Observe and filter history records
        viewModel.allScans.observe(viewLifecycleOwner) { scans ->
            allScansList = scans
            val query = binding.etSearch.text?.toString()?.lowercase() ?: ""
            val filtered = if (query.isEmpty()) {
                scans
            } else {
                scans.filter { 
                    it.target.lowercase().contains(query) || 
                    it.type.lowercase().contains(query) 
                }
            }
            adapter.submitList(filtered)
            binding.llEmpty.visibility = if (scans.isEmpty()) View.VISIBLE else View.GONE
        }

        // Update storage utilization dashboard
        viewModel.storageInfo.observe(viewLifecycleOwner) { info ->
            binding.tvFilesStored.text = getString(R.string.files_stored_format, info.fileCount)
            binding.tvStorageUsed.text = formatFileSize(info.totalSizeBytes)
            binding.tvLastScannedFile.text = getString(R.string.last_scan_format, info.lastFileName)
        }
    }

    /**
     * Helper to format raw bytes into a human-readable string (e.g., 2.4 MB).
     */
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}