package com.cerebus.readwrite

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cerebus.create_screen.navigation.CreateNavigationState
import com.cerebus.readwrite.navigation.StudentImportNavigationState
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (rerouteExternalArchiveIfNeeded(intent)) {
            finish()
            return
        }
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

    private fun rerouteExternalArchiveIfNeeded(intent: Intent?): Boolean {
        val archiveUri = extractSupportedArchiveUri(intent) ?: return false
        if (isTaskRoot) return false

        val reroutedIntent = Intent(intent).apply {
            setClass(this@MainActivity, MainActivity::class.java)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            data = archiveUri
        }
        startActivity(reroutedIntent)
        return true
    }

    private fun handleIncomingDeckArchiveIntent(intent: Intent?) {
        val archiveUri = extractSupportedArchiveUri(intent) ?: return
        when (detectArchiveKind(archiveUri, intent?.type)) {
            ArchiveKind.DECK -> CreateNavigationState.requestImportDeckArchive(archiveUri.toString())
            ArchiveKind.STUDENT -> StudentImportNavigationState.requestImportStudentArchive(archiveUri.toString())
            null -> Unit
        }
    }

    private fun extractSupportedArchiveUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getStreamUri()
            else -> null
        }?.takeIf { detectArchiveKind(it, intent.type) != null }
    }

    private fun detectArchiveKind(
        uri: Uri,
        mimeType: String?,
    ): ArchiveKind? {
        val normalizedMime = mimeType?.lowercase(Locale.US)
        if (
            normalizedMime == "application/x-rwdeck" ||
            normalizedMime == "application/vnd.readwrite.deck+zip"
        ) {
            return ArchiveKind.DECK
        }
        if (
            normalizedMime == "application/x-rwstudent" ||
            normalizedMime == "application/vnd.readwrite.student+zip"
        ) {
            return ArchiveKind.STUDENT
        }

        val candidatePath = (uri.lastPathSegment ?: uri.path ?: uri.toString())
            .lowercase(Locale.US)
        return when {
            candidatePath.endsWith(".rwdeck") -> ArchiveKind.DECK
            candidatePath.endsWith(".rwstudent") -> ArchiveKind.STUDENT
            else -> null
        }
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

private enum class ArchiveKind {
    DECK,
    STUDENT,
}

@Preview
@Composable
fun AppAndroidPreview() {
    ReadWriteAppNavigation()
}
