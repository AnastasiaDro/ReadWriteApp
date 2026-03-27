package com.cerebus.readwrite.student

import com.cerebus.core.deck_package.domain.service.DeckImportMode
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.utils.CustomResult
import com.cerebus.readwrite.view.StarterDeckInstaller
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import readwriteapp.composeapp.generated.resources.Res

private const val STARTER_DECK_RESOURCE_PATH = "files/starter_decks/family_starter.rwdeck"

class IosStarterDeckInstaller(
    private val deckPackageService: DeckPackageService,
) : StarterDeckInstaller {

    @OptIn(ExperimentalForeignApi::class, ExperimentalResourceApi::class)
    override suspend fun installStarterDeck(studentId: String): CustomResult<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val archiveBytes = Res.readBytes(STARTER_DECK_RESOURCE_PATH)
            val directoryPath = "${NSTemporaryDirectory().trimEnd('/')}/starter_decks"
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = directoryPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            val filePath = "$directoryPath/family_starter_$studentId.rwdeck"
            val file = fopen(filePath, "wb") ?: error("Unable to create starter deck temp file")
            try {
                if (archiveBytes.isNotEmpty()) {
                    archiveBytes.usePinned { pinned ->
                        fwrite(
                            pinned.addressOf(0),
                            1.convert(),
                            archiveBytes.size.convert(),
                            file,
                        )
                    }
                }
            } finally {
                fclose(file)
            }

            try {
                when (
                    val result = deckPackageService.importDeck(
                        archiveUri = filePath,
                        assignToStudentId = studentId,
                        mode = DeckImportMode.ADD_MISSING_CARDS,
                    )
                ) {
                    is CustomResult.Success -> Unit
                    is CustomResult.Failure -> throw result.error
                }
            } finally {
                NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
            }
        }.fold(
            onSuccess = { CustomResult.Success(Unit) },
            onFailure = { CustomResult.Failure(it) },
        )
    }
}
