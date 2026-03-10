package com.cerebus.readwrite

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cerebus.create_screen.navigation.CreateNavigationState
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIncomingDeckArchiveIntent(intent)
        }

        setContent {
            ReadWriteAppNavigation()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDeckArchiveIntent(intent)
    }

    private fun handleIncomingDeckArchiveIntent(intent: Intent?) {
        val archiveUri = extractDeckArchiveUri(intent) ?: return
        CreateNavigationState.requestImportDeckArchive(archiveUri.toString())
    }

    private fun extractDeckArchiveUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getStreamUri()
            else -> null
        }?.takeIf { isDeckArchiveUri(it, intent.type) }
    }

    private fun isDeckArchiveUri(
        uri: Uri,
        mimeType: String?,
    ): Boolean {
        val normalizedMime = mimeType?.lowercase(Locale.US)
        if (
            normalizedMime == "application/x-rwdeck" ||
            normalizedMime == "application/vnd.readwrite.deck+zip"
        ) {
            return true
        }

        val candidatePath = (uri.lastPathSegment ?: uri.path ?: uri.toString())
            .lowercase(Locale.US)
        return candidatePath.endsWith(".rwdeck")
    }

    @Suppress("DEPRECATION")
    private fun Intent.getStreamUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ReadWriteAppNavigation()
}
