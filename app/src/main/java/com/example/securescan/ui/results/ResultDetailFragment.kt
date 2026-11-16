package com.example.securescan.ui.results

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securescan.R
import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.databinding.FragmentResultDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ResultDetailFragment : Fragment() {

    private var _binding: FragmentResultDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResultDetailViewModel by viewModels()
    private val args: ResultDetailFragmentArgs by navArgs()

    private lateinit var adapter: EngineResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupExpandCollapse()
        observeViewModel()
        
        viewModel.fetchAnalysis(args.analysisId)
    }

    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
        binding.toolbar.inflateMenu(R.menu.menu_result_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_report_title)
            .setMessage(R.string.delete_report_message)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteScan(args.analysisId)
                Toast.makeText(requireContext(), R.string.report_deleted, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = EngineResultAdapter(emptyList())
        binding.rvEngineResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEngineResults.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchEngines.addTextChangedListener { text ->
            adapter.filter(text?.toString() ?: "")
        }
    }

    private fun setupExpandCollapse() {
        binding.llEnginesHeader.setOnClickListener {
            val isVisible = binding.llEnginesContent.visibility == View.VISIBLE
            binding.llEnginesContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivExpand.animate().rotation(if (isVisible) -90f else 0f).start()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.analysisResult.collectLatest { state ->
                    when (state) {
                        is ResultDetailViewModel.UiState.Loading -> {
                            // Show loading
                        }
                        is ResultDetailViewModel.UiState.Success -> {
                            updateUi(state.data)
                        }
                        is ResultDetailViewModel.UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUi(analysis: AnalysisResponse) {
        val attributes = analysis.data.attributes ?: return
        val stats = attributes.stats
        
        val totalEngines = stats.malicious + stats.suspicious + stats.harmless + stats.undetected
        
        // 1. Scoring Logic & Dynamic Data
        val verdictText: String
        val colorHex: String
        val riskLabel: String
        val evidenceTitle: String
        val riskScore: Int
        
        when {
            stats.malicious > 0 -> {
                verdictText = getString(R.string.verdict_malicious)
                colorHex = "#FF3B30" // Red
                riskLabel = if (stats.malicious > 5) getString(R.string.risk_critical) else getString(R.string.risk_high)
                evidenceTitle = getString(R.string.evidence_malicious_title)
                // Score logic: 0-40
                riskScore = if (stats.malicious > 5) 15 else 35
            }
            stats.suspicious > 0 -> {
                verdictText = getString(R.string.verdict_suspicious)
                colorHex = "#FFCC00" // Yellow
                riskLabel = getString(R.string.risk_low) // Or "Suspicious" if we had a string
                evidenceTitle = getString(R.string.evidence_suspicious_title)
                riskScore = 55
            }
            else -> {
                verdictText = getString(R.string.verdict_safe)
                colorHex = "#4CD964" // Green
                riskLabel = getString(R.string.risk_minimal)
                evidenceTitle = getString(R.string.evidence_safe_title)
                riskScore = 98
            }
        }

        val themeColor = Color.parseColor(colorHex)

        // Banner
        binding.verdictCard.setCardBackgroundColor(adjustAlpha(themeColor, 0.15f))
        binding.verdictCard.strokeColor = themeColor
        binding.tvVerdictTitle.text = verdictText
        binding.tvVerdictTitle.setTextColor(themeColor)
        binding.ivVerdictIcon.setColorFilter(themeColor)

        // Score Card
        binding.scoreGauge.progress = riskScore
        binding.scoreGauge.setIndicatorColor(themeColor)
        binding.tvScoreValue.text = riskScore.toString()
        binding.tvScoreLabel.text = if (riskScore > 80) "SAFE" else riskLabel.uppercase()
        binding.tvScoreLabel.setTextColor(themeColor)

        binding.tvThreatLevel.text = riskLabel.uppercase()
        binding.tvThreatLevel.setTextColor(themeColor)
        
        binding.tvDetectionRatio.text = getString(R.string.detection_ratio_format, stats.malicious, totalEngines)
        binding.tvReputation.text = attributes.reputation?.toString() ?: "0"
        binding.tvReputation.setTextColor(if ((attributes.reputation ?: 0) < 0) Color.parseColor("#FF3B30") else Color.parseColor("#4CD964"))

        // Evidence Section
        binding.tvEvidenceTitle.text = evidenceTitle
        binding.llSafeChecks.removeAllViews()
        
        if (stats.malicious > 0) {
            addEvidenceItem("🚨 Security vendors detected malicious activity", Color.parseColor("#FF3B30"))
            addEvidenceItem("🚨 Malware or phishing indicators were identified", Color.parseColor("#FF3B30"))
            addEvidenceItem("🚨 High security risk detected", Color.parseColor("#FF3B30"))
            addEvidenceItem("🚨 Avoid interacting with this item", Color.parseColor("#FF3B30"))
        } else if (stats.suspicious > 0) {
            addEvidenceItem("⚠ Some security vendors reported suspicious behavior", Color.parseColor("#FFCC00"))
            addEvidenceItem("⚠ Additional verification is recommended", Color.parseColor("#FFCC00"))
            addEvidenceItem("⚠ Potential risk indicators were detected", Color.parseColor("#FFCC00"))
        } else {
            addEvidenceItem("✓ No malicious detections", Color.parseColor("#4CD964"))
            addEvidenceItem("✓ No suspicious detections", Color.parseColor("#4CD964"))
            addEvidenceItem("✓ $totalEngines security engines analyzed the item", Color.parseColor("#4CD964"))
            addEvidenceItem("✓ No blacklist matches found", Color.parseColor("#4CD964"))
        }

        // Summary Progress
        binding.tvCountHarmless.text = stats.harmless.toString()
        binding.progressHarmless.max = totalEngines
        binding.progressHarmless.setProgressCompat(stats.harmless, true)

        binding.tvCountMalicious.text = stats.malicious.toString()
        binding.progressMalicious.max = totalEngines
        binding.progressMalicious.setProgressCompat(stats.malicious, true)

        binding.tvCountSuspicious.text = stats.suspicious.toString()
        binding.progressSuspicious.max = totalEngines
        binding.progressSuspicious.setProgressCompat(stats.suspicious, true)

        // Tech Info Card
        setupTechInfo(analysis)

        // Engines
        val engineResults = attributes.results.values.toList()
        binding.tvEngineCount.text = getString(R.string.engines_count, engineResults.size)
        adapter.updateResults(engineResults)
    }

    private fun setupTechInfo(analysis: AnalysisResponse) {
        binding.llTechRows.removeAllViews()
        val type = args.scanType
        binding.tvTechTitle.text = if (type == "URL") "DOMAIN INTELLIGENCE" else "FILE INTELLIGENCE"
        
        if (type == "URL") {
            addTechRow("Type", "URL / Domain")
            addTechRow("ID", analysis.data.id)
            addTechRow("Status", analysis.data.attributes?.status?.uppercase() ?: "UNKNOWN")
        } else {
            addTechRow("Type", "File Analysis")
            val hash = analysis.data.id.split("-").firstOrNull() ?: analysis.data.id
            addTechRow("SHA256", hash)
            addTechRow("Method", "Cloud Sandboxing")
        }
        
        val date = analysis.data.attributes?.date ?: 0L
        if (date > 0) {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            addTechRow("Analyzed", sdf.format(Date(date * 1000)))
        }
    }

    private fun addEvidenceItem(text: String, color: Int) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_security_check, binding.llSafeChecks, false)
        view.findViewById<TextView>(R.id.tv_check_text).apply {
            this.text = text
            setTextColor(color)
        }
        view.findViewById<ImageView>(R.id.iv_check_icon).apply {
            setColorFilter(color)
            setImageResource(if (text.startsWith("✓")) R.drawable.ic_check_circle else R.drawable.ic_securescan_logo)
        }
        binding.llSafeChecks.addView(view)
    }

    private fun addTechRow(label: String, value: String) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 8.dp)
            }
        }
        val tvLabel = TextView(requireContext()).apply {
            text = getString(R.string.tech_row_label, label)
            setTextColor(Color.parseColor("#8E8E93"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(100.dp, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val tvValue = TextView(requireContext()).apply {
            text = value
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                copyToClipboard(label, value)
            }
        }
        layout.addView(tvLabel)
        layout.addView(tvValue)
        binding.llTechRows.addView(layout)
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}