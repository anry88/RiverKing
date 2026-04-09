package com.riverking.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.GoogleSignInManager
import com.riverking.mobile.auth.SecureSessionStore
import com.riverking.mobile.ui.RiverKingApp
import com.riverking.mobile.ui.RiverKingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember {
                AuthRepository(
                    sessionStore = SecureSessionStore(applicationContext),
                )
            }
            val googleSignInManager = remember { GoogleSignInManager() }
            val viewModel = remember { RiverKingViewModel(repository) }
            RiverKingApp(
                viewModel = viewModel,
                googleSignInManager = googleSignInManager,
                activity = this,
            )
        }
    }
}
