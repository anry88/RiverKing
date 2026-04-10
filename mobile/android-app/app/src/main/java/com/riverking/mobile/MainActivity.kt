package com.riverking.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.GoogleSignInManager
import com.riverking.mobile.auth.SecureSessionStore
import com.riverking.mobile.ui.RiverKingApp
import com.riverking.mobile.ui.RiverKingViewModel

class MainActivity : ComponentActivity() {
    private lateinit var repository: AuthRepository
    private lateinit var googleSignInManager: GoogleSignInManager
    private lateinit var viewModel: RiverKingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AuthRepository(
            sessionStore = SecureSessionStore(applicationContext),
        )
        googleSignInManager = GoogleSignInManager()
        applyTelegramReturnIntent(intent, resumeImmediately = false)
        viewModel = RiverKingViewModel(repository)
        enableEdgeToEdge()
        setContent {
            RiverKingApp(
                viewModel = viewModel,
                googleSignInManager = googleSignInManager,
                activity = this,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyTelegramReturnIntent(intent, resumeImmediately = true)
    }

    private fun applyTelegramReturnIntent(
        intent: Intent?,
        resumeImmediately: Boolean,
    ) {
        val data = intent?.data ?: return
        if (data.scheme != "riverking" || data.host != "auth") return
        val token = data.getQueryParameter("token")?.trim()?.takeIf { it.isNotEmpty() } ?: return
        when (data.lastPathSegment) {
            "login" -> {
                repository.rememberPendingTelegramLogin(token)
                if (resumeImmediately && ::viewModel.isInitialized) {
                    viewModel.resumeTelegramLogin(token)
                }
            }
            "link" -> {
                repository.rememberPendingTelegramLink(token)
                if (resumeImmediately && ::viewModel.isInitialized) {
                    viewModel.resumeTelegramLink(token)
                }
            }
        }
    }
}
