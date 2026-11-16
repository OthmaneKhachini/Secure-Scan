package com.example.securescan.ui.filescanner

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.securescan.R
import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.databinding.FragmentFileScannerBinding
import com.example.securescan.util.FileUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment responsible for initiating local file security scans.
 * Uses a pulse animation for a high-tech "scanning" aesthetic.
 */
@AndroidEntryPoint
class FileScannerFragment : Fragment() {

    private var _binding: FragmentFileScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FileScannerViewModel by viewModels()
    private var selectedFileUri: Uri? = null

    /**
     * Launcher for the system file picker content contract.
     */
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            binding.btnScanFile.visibility = View.VISIBLE
            binding.tvSelectedFile.apply {
                visibility = View.VISIBLE
                text = it.lastPathSegment ?: "Selected File"
            }
            binding.tvHint.text = "FILE PREPARED"
            Toast.makeText(requireContext(), "File ready for cloud analysis", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startPulseAnimation()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.cvUpload.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnScanFile.setOnClickListener {
            selectedFileUri?.let { uri ->
                try {
                    val payload = FileUtils.getFilePayload(requireContext(), uri, "file")
                    if (payload != null) {
                        viewModel.scanFile(payload.part, payload.file)
                    } else {
                        Toast.makeText(requireContext(), "Failed to process file for upload", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Please select a file first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Recursively starts a pulse animation on the background circle.
     */
    private fun startPulseAnimation() {
        _binding?.vPulse?.apply {
            alpha = 0.4f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .scaleX(2.0f)
                .scaleY(2.0f)
                .alpha(0f)
                .setDuration(1500)
                .withEndAction { 
                    if (isAdded && _binding != null) startPulseAnimation() 
                }
                .start()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect analysis result state
                launch {
                    viewModel.scanResult.collectLatest { state ->
                        handleUiState(state)
                    }
                }
                // Collect real-time scanning status text
                launch {
                    viewModel.scanStatus.collectLatest { status ->
                        binding.tvScanStatus.text = status ?: "Processing..."
                    }
                }
            }
        }
    }

    private fun handleUiState(state: FileScannerViewModel.UiState<AnalysisResponse>) {
        when (state) {
            is FileScannerViewModel.UiState.Loading -> {
                binding.btnScanFile.isEnabled = false
                binding.cvUpload.isEnabled = false
                binding.llScanningStatus.visibility = View.VISIBLE
            }
            is FileScannerViewModel.UiState.Success -> {
                binding.btnScanFile.isEnabled = true
                binding.cvUpload.isEnabled = true
                binding.llScanningStatus.visibility = View.GONE
                
                val analysisId = state.data.data.id
                viewModel.resetState()

                val action = FileScannerFragmentDirections
                    .actionFileScannerFragmentToResultDetailFragment(analysisId, "FILE")
                findNavController().navigate(action)
            }
            is FileScannerViewModel.UiState.Error -> {
                binding.btnScanFile.isEnabled = true
                binding.cvUpload.isEnabled = true
                binding.llScanningStatus.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
            else -> {
                binding.llScanningStatus.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}