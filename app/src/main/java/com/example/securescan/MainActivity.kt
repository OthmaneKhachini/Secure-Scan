package com.example.securescan

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.securescan.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * The primary activity and entry point of the SecureScan application.
 * Manages the top-level navigation and system UI visibility.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure status bar is transparent/matching background
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Setup Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle window insets for floating navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.navCard.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Manage Bottom Navigation visibility based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.resultDetailFragment) {
                hideBottomNav()
            } else {
                showBottomNav()
            }
        }
    }

    private fun hideBottomNav() {
        binding.navCard.animate()
            .alpha(0f)
            .translationY(binding.navCard.height.toFloat() + 100f)
            .setDuration(300)
            .withEndAction { binding.navCard.visibility = View.GONE }
            .start()
    }

    private fun showBottomNav() {
        binding.navCard.visibility = View.VISIBLE
        binding.navCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .start()
    }
}