package com.example.securescan.ui.urlscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.databinding.FragmentUrlScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment responsible for initiating URL-based threat analysis.
 * Features an interactive UI with real-time scanning status updates.
 */
@AndroidEntryPoint
class UrlScannerFragment : Fragment() {

    private var _binding: FragmentUrlScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UrlScannerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUrlScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startPulseAnimation()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                viewModel.scanUrl(url)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show()
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
                launch {
                    viewModel.scanResult.collectLatest { state ->
                        handleUiState(state)
                    }
                }
                launch {
                    viewModel.scanStatus.collectLatest { status ->
                        binding.tvScanStatus.text = status ?: "Awaiting submission..."
                    }
                }
            }
        }
    }

    private fun handleUiState(state: UrlScannerViewModel.UiState<AnalysisResponse>) {
        when (state) {
            is UrlScannerViewModel.UiState.Loading -> {
                binding.btnScan.isEnabled = false
                binding.llScanningStatus.visibility = View.VISIBLE
            }
            is UrlScannerViewModel.UiState.Success -> {
                binding.btnScan.isEnabled = true
                binding.llScanningStatus.visibility = View.GONE
                
                val analysisId = state.data.data.id
                viewModel.resetState()
                
                val action = UrlScannerFragmentDirections
                    .actionUrlScannerFragmentToResultDetailFragment(analysisId, "URL")
                findNavController().navigate(action)
            }
            is UrlScannerViewModel.UiState.Error -> {
                binding.btnScan.isEnabled = true
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