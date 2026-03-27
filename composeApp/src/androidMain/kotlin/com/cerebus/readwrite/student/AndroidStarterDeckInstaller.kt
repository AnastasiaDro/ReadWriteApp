package com.cerebus.readwrite.student

import android.content.Context
import com.cerebus.core.deck_package.domain.service.DeckImportMode
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.utils.CustomResult
import com.cerebus.readwrite.view.StarterDeckInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import readwriteapp.composeapp.generated.resources.Res
import java.io.File

private const val STARTER_DECK_RESOURCE_PATH = "files/starter_decks/family_starter.rwdeck"

class AndroidStarterDeckInstaller(
    private val appContext: Context,
    private val deckPackageService: DeckPackageService,
) : StarterDeckInstaller {

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun installStarterDeck(studentId: String): CustomResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val archiveBytes = Res.readBytes(STARTER_DECK_RESOURCE_PATH)
            val tempDir = File(appContext.cacheDir, "starter_decks").apply { mkdirs() }
            val tempFile = File(tempDir, "family_starter_$studentId.rwdeck")
            tempFile.writeBytes(archiveBytes)
            try {
                when (
                    val result = deckPackageService.importDeck(
                        archiveUri = tempFile.absolutePath,
                        assignToStudentId = studentId,
                        mode = DeckImportMode.ADD_MISSING_CARDS,
                    )
                ) {
                    is CustomResult.Success -> Unit
                    is CustomResult.Failure -> throw result.error
                }
            } finally {
                tempFile.delete()
            }
        }.fold(
            onSuccess = { CustomResult.Success(Unit) },
            onFailure = { CustomResult.Failure(it) },
        )
    }
}
