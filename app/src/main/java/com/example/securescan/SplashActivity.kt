package com.example.securescan

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.securescan.databinding.ActivitySplashBinding

/**
 * Handles the initial application branding and initialization phase.
 */
class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        runBrandingAnimations()
    }

    /**
     * Executes the premium entrance animations for the logo and app name.
     */
    private fun runBrandingAnimations() {
        binding.ivLogo.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            
            animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(1200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(400)
                        .start()
                }
                .start()
        }

        binding.tvAppName.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(600)
                .start()
        }

        binding.progressBar.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(1000)
                .withEndAction {
                    navigateToMain()
                }
                .start()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}