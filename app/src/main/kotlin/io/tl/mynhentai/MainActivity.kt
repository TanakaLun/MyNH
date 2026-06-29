package io.tl.mynhentai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.tl.mynhentai.ui.navigation.MainNavGraph
import io.tl.mynhentai.ui.theme.MyNHentaiTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    var pendingDeepLink: String? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        handleIntent(intent)
        setContent {
            MyNHentaiTheme {
                MainNavGraph(
                    initialDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    val host = uri.host
                    val path = uri.pathSegments
                    if (host == "nhentai.net" && path.size >= 2 && path[0] == "g") {
                        val id = path[1].toLongOrNull()
                        if (id != null) {
                            pendingDeepLink = "gallery/$id"
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                val regex = Regex("""nhentai\.net/g/(\d+)""")
                val match = regex.find(text)
                if (match != null) {
                    val id = match.groupValues[1].toLongOrNull()
                    if (id != null) {
                        pendingDeepLink = "gallery/$id"
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
