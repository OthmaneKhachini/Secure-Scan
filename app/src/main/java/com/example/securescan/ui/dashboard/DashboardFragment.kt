package com.example.securescan.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.securescan.R
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment representing the main security dashboard.
 * Displays real-time protection status, metrics, and recent activity.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimations()
        setupChart()
        observeViewModel()
        setupClickListeners()
    }

    /**
     * Initializes entrance animations for the dashboard components.
     */
    private fun setupAnimations() {
        // Hero Card Entrance
        binding.heroCard.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Staggered entrance for quick actions
        val quickActions = listOf(
            binding.btnQuickUrl,
            binding.btnQuickFile,
            binding.btnQuickReports
        )
        
        quickActions.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(400L + (index * 100))
                .start()
        }
    }

    /**
     * Configures the weekly security activity chart.
     */
    private fun setupChart() {
        // Placeholder data for the visual trend
        val entries = listOf(
            Entry(0f, 4f), Entry(1f, 3f), Entry(2f, 7f), 
            Entry(3f, 5f), Entry(4f, 12f), Entry(5f, 6f), Entry(6f, 10f)
        )
        
        val dataSet = LineDataSet(entries, "Security Activity").apply {
            color = Color.parseColor("#00D2FF")
            setCircleColor(Color.WHITE)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#00D2FF")
            fillAlpha = 40
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        binding.securityChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            axisLeft.apply {
                textColor = Color.parseColor("#8E8E93")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1AFFFFFF")
                textSize = 9f
            }
            setTouchEnabled(false)
            animateXY(1200, 1200)
            invalidate()
        }
    }

    private fun observeViewModel() {
        // Observe statistical metrics
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats.total > 0) {
                binding.llStatsContainer.visibility = View.VISIBLE
                binding.tvTotalScansCount.text = stats.total.toString()
                binding.tvThreatsCount.text = stats.threats.toString()
                binding.tvSafeUrls.text = stats.safeUrls.toString()
                binding.tvSafeFiles.text = stats.safeFiles.toString()
            } else {
                binding.llStatsContainer.visibility = View.GONE
            }
        }

        // Observe chronological activity
        viewModel.recentActivity.observe(viewLifecycleOwner) { activity ->
            updateRecentActivity(activity)
            updateSecurityOverview(activity.firstOrNull())
        }
    }

    /**
     * Dynamically updates the security overview card based on the most recent scan.
     */
    private fun updateSecurityOverview(latest: ScanEntity?) {
        if (latest == null) {
            binding.tvStatus.text = getString(R.string.status_ready)
            binding.tvStatus.setTextColor(Color.parseColor("#00D2FF"))
            binding.tvRiskLevel.text = "None"
            binding.tvScoreValue.text = "--"
            binding.scoreGauge.progress = 0
            binding.tvLastScanInfo.text = "No scans performed yet"
            return
        }

        val malicious = latest.maliciousCount
        val score: Int
        val status: String
        val colorHex: String
        val risk: String

        when {
            malicious > 5 -> {
                score = 15
                status = getString(R.string.status_critical)
                colorHex = "#FF3B30"
                risk = getString(R.string.risk_critical)
            }
            malicious > 0 -> {
                score = 45
                status = getString(R.string.status_at_risk)
                colorHex = "#FFCC00"
                risk = getString(R.string.risk_high)
            }
            else -> {
                score = 98
                status = getString(R.string.status_protected)
                colorHex = "#4CD964"
                risk = getString(R.string.risk_minimal)
            }
        }

        val themeColor = Color.parseColor(colorHex)
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(themeColor)
        binding.tvRiskLevel.text = risk
        binding.tvRiskLevel.setTextColor(themeColor)
        binding.tvScoreValue.text = score.toString()
        binding.scoreGauge.setIndicatorColor(themeColor)
        binding.scoreGauge.setProgressCompat(score, true)

        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(latest.date))
        val resultStr = if (malicious > 0) "Threats Found" else "Safe"
        binding.tvLastScanInfo.text = getString(R.string.last_scan_info_format, timeStr, resultStr)
    }

    /**
     * Populates the recent activity feed with color-coded status badges.
     */
    private fun updateRecentActivity(scans: List<ScanEntity>) {
        binding.llRecentActivity.removeAllViews()
        
        if (scans.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = getString(R.string.no_recent_activity)
                setTextColor(Color.parseColor("#8E8E93"))
                textSize = 12f
                setPadding(24.dp, 24.dp, 24.dp, 24.dp)
            }
            binding.llRecentActivity.addView(emptyTv)
            return
        }

        scans.forEach { scan ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_timeline, binding.llRecentActivity, false)
            val isMalicious = scan.maliciousCount > 0
            val color = if (isMalicious) Color.parseColor("#FF3B30") else Color.parseColor("#4CD964")
            
            row.findViewById<TextView>(R.id.tv_timeline_title).text = scan.target
            val statusStr = if (isMalicious) "🔴 Malicious" else "🟢 Safe"
            val timeStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(scan.date))
            
            row.findViewById<TextView>(R.id.tv_timeline_subtitle).text = "$statusStr • $timeStr"
            row.findViewById<View>(R.id.v_status_dot)?.backgroundTintList = ColorStateList.valueOf(color)
            
            binding.llRecentActivity.addView(row)
        }
    }

    private fun setupClickListeners() {
        binding.btnQuickUrl.setOnClickListener {
            findNavController().navigate(R.id.urlScannerFragment)
        }
        binding.btnQuickFile.setOnClickListener {
            findNavController().navigate(R.id.fileScannerFragment)
        }
        binding.btnQuickReports.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}