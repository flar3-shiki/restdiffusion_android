package jp.kira.sdwebuiremote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.kira.sdwebuiremote.data.ThemeSetting
import jp.kira.sdwebuiremote.ui.AppRoot
import jp.kira.sdwebuiremote.ui.MainViewModel
import jp.kira.sdwebuiremote.ui.MainViewModelFactory
import jp.kira.sdwebuiremote.ui.theme.SDWebUIRemoteTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission grant result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(application)
            )

            if (intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
                val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                }
                mainViewModel.handleSharedImage(imageUri)
            }

            val themeSetting by mainViewModel.themeSetting.collectAsState()

            SDWebUIRemoteTheme(
                darkTheme = when (themeSetting) {
                    ThemeSetting.System -> isSystemInDarkTheme()
                    ThemeSetting.Light -> false
                    ThemeSetting.Dark -> true
                },
                dynamicColor = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(mainViewModel = mainViewModel)
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
            }
            val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
            viewModel.handleSharedImage(imageUri)
        }
    }
}