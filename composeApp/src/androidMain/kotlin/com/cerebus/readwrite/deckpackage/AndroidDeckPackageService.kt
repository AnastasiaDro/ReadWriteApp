package com.cerebus.readwrite.deckpackage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.cerebus.core.deck_package.domain.model.DeckPackageCard
import com.cerebus.core.deck_package.domain.model.DeckPackageDeck
import com.cerebus.core.deck_package.domain.model.DeckPackageManifest
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaAsset
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaKind
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaRef
import com.cerebus.core.deck_package.domain.model.RW_DECK_FORMAT
import com.cerebus.core.deck_package.domain.model.RW_DECK_SCHEMA_VERSION
import com.cerebus.core.deck_package.domain.service.DeckPackageExportFile
import com.cerebus.core.deck_package.domain.service.DeckImportMode
import com.cerebus.core.deck_package.domain.service.DeckPackageImportPreview
import com.cerebus.core.deck_package.domain.service.DeckPackageImportResult
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val MANIFEST_FILE_NAME = "manifest.json"
private const val MEDIA_DIR_NAME = "media"

class AndroidDeckPackageService(
    private val appContext: Context,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val studentDeckRepository: StudentDeckRepository,
) : DeckPackageService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun exportDeck(deckId: String): CustomResult<DeckPackageExportFile> = withContext(Dispatchers.IO) {
        runCatching {
            val deck = deckRepository.getDeckById(deckId)
                ?: error("Deck not found: $deckId")
            val cards = flashcardRepository.getFlashcardsByDeckId(deckId)

            val workDir = createWorkDir(prefix = "rwdeck_export_")
            try {
                val mediaAssets = mutableListOf<DeckPackageMediaAsset>()
                val coverMedia = deck.coverUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { sourceUri ->
                        exportMedia(
                            sourceUri = sourceUri,
                            workDir = workDir,
                            mediaAssets = mediaAssets,
                            preferredPrefix = "deck_cover",
                            fallbackKind = DeckPackageMediaKind.IMAGE,
                        )
                    }

                val cardEntries = cards.mapIndexed { index, card ->
                    val media = card.imageUrl
                        .takeIf { it.isNotBlank() }
                        ?.let { sourceUri ->
                            exportMedia(
                                sourceUri = sourceUri,
                                workDir = workDir,
                                mediaAssets = mediaAssets,
                                preferredPrefix = "card_${index + 1}",
                                fallbackKind = DeckPackageMediaKind.IMAGE,
                            )
                        }

                    DeckPackageCard(
                        sourceCardId = card.id,
                        text = card.name,
                        media = media,
                        position = card.position,
                    )
                }

                val manifest = DeckPackageManifest(
                    exportedAtEpochMillis = nowMillis(),
                    deck = DeckPackageDeck(
                        sourceDeckId = deck.id,
                        name = deck.name,
                        coverMedia = coverMedia,
                    ),
                    cards = cardEntries,
                    media = mediaAssets,
                )

                val manifestFile = File(workDir, MANIFEST_FILE_NAME)
                manifestFile.writeText(json.encodeToString(DeckPackageManifest.serializer(), manifest))

                val preferredDownloadRoot = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val outputDir = File(preferredDownloadRoot ?: appContext.filesDir, "deck_packages").apply { mkdirs() }
                val safeDeckName = sanitizeForFileName(deck.name)
                val fileName = "${safeDeckName}_${nowMillis()}.rwdeck"
                val outputFile = File(outputDir, fileName)

                zipDirectory(workDir = workDir, outputFile = outputFile)

                DeckPackageExportFile(
                    path = outputFile.absolutePath,
                    fileName = outputFile.name,
                )
            } finally {
                workDir.deleteRecursively()
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    override suspend fun inspectDeckImport(
        archiveUri: String,
    ): CustomResult<DeckPackageImportPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceArchive = copyArchiveToTemp(archiveUri)
            val unzipDir = createWorkDir(prefix = "rwdeck_inspect_")

            try {
                unzipArchive(sourceArchive, unzipDir)

                val manifestFile = File(unzipDir, MANIFEST_FILE_NAME)
                if (!manifestFile.exists()) {
                    error("manifest.json is missing in archive")
                }
                val manifest = json.decodeFromString(
                    DeckPackageManifest.serializer(),
                    manifestFile.readText(),
                )
                validateManifest(manifest)

                val existingDeck = manifest.deck.sourceDeckId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { deckId -> deckRepository.getDeckById(deckId) }
                val existingCardsById = existingDeck
                    ?.let { flashcardRepository.getFlashcardsByDeckId(it.id).associateBy { card -> card.id } }
                    .orEmpty()
                val importedCardIds = manifest.cards.mapNotNull { card ->
                    card.sourceCardId?.takeIf { it.isNotBlank() }
                }.toSet()
                val matchingCardsCount = importedCardIds.count { cardId -> existingCardsById.containsKey(cardId) }
                val newCardsCount = importedCardIds.count { cardId -> !existingCardsById.containsKey(cardId) } +
                    manifest.cards.count { card -> card.sourceCardId.isNullOrBlank() }
                val staleCardsCount = existingCardsById.keys.count { cardId -> cardId !in importedCardIds }

                DeckPackageImportPreview(
                    deckName = manifest.deck.name,
                    existingDeckId = existingDeck?.id,
                    existingDeckName = existingDeck?.name,
                    matchingCardsCount = matchingCardsCount,
                    newCardsCount = newCardsCount,
                    staleCardsCount = staleCardsCount,
                )
            } finally {
                sourceArchive.delete()
                unzipDir.deleteRecursively()
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    override suspend fun importDeck(
        archiveUri: String,
        assignToStudentId: String?,
        mode: DeckImportMode,
    ): CustomResult<DeckPackageImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceArchive = copyArchiveToTemp(archiveUri)
            val unzipDir = createWorkDir(prefix = "rwdeck_import_")

            try {
                unzipArchive(sourceArchive, unzipDir)

                val manifestFile = File(unzipDir, MANIFEST_FILE_NAME)
                if (!manifestFile.exists()) {
                    error("manifest.json is missing in archive")
                }
                val manifest = json.decodeFromString(
                    DeckPackageManifest.serializer(),
                    manifestFile.readText(),
                )
                validateManifest(manifest)

                val sourceDeckId = manifest.deck.sourceDeckId?.takeIf { it.isNotBlank() }
                val existingDeck = if (sourceDeckId != null) {
                    deckRepository.getDeckById(sourceDeckId)
                } else {
                    null
                }
                val targetDeckId = existingDeck?.id
                    ?: sourceDeckId
                    ?: UniqueIdGenerator.randomAlphanumeric(prefix = "deck")
                var createdDeckIdForRollback: String? = null

                try {
                    val coverUri = manifest.deck.coverMedia?.let { media ->
                        importMediaAndGetLocalUri(
                            unzipDir = unzipDir,
                            mediaFile = media.file,
                        )
                    }

                    if (existingDeck == null) {
                        val createdDeck = deckRepository.addDeck(
                            Deck(
                                id = targetDeckId,
                                name = manifest.deck.name,
                                coverUri = coverUri,
                            )
                        )
                        if (!createdDeck) {
                            error("Failed to create deck from archive")
                        }
                        createdDeckIdForRollback = targetDeckId
                    } else if (mode == DeckImportMode.REPLACE_EXISTING) {
                        val updatedName = deckRepository.updateDeckName(
                            id = targetDeckId,
                            name = manifest.deck.name,
                        )
                        val updatedCover = deckRepository.updateDeckCoverUri(
                            id = targetDeckId,
                            coverUri = coverUri,
                        )
                        if (!updatedName || !updatedCover) {
                            error("Failed to update imported deck")
                        }
                    }

                    val existingCards = flashcardRepository.getFlashcardsByDeckId(targetDeckId)
                    val existingCardsById = existingCards.associateBy { it.id }
                    val importedCardIds = mutableSetOf<String>()
                    var nextAppendPosition = (existingCards.maxOfOrNull(Flashcard::position) ?: -1) + 1

                    var importedCardsCount = 0
                    manifest.cards.forEachIndexed { index, card ->
                        val targetCardId = card.sourceCardId?.takeIf { it.isNotBlank() }
                            ?: UniqueIdGenerator.randomAlphanumeric(prefix = "card")
                        val existingCard = existingCardsById[targetCardId]
                        val shouldReplaceExisting = mode == DeckImportMode.REPLACE_EXISTING
                        if (shouldReplaceExisting) {
                            importedCardIds += targetCardId
                        }
                        if (existingCard != null && !shouldReplaceExisting) {
                            return@forEachIndexed
                        }
                        val targetPosition = if (shouldReplaceExisting) {
                            card.position ?: index
                        } else {
                            nextAppendPosition++
                            nextAppendPosition - 1
                        }

                        val cardMediaUri = card.media?.let { media ->
                            importMediaAndGetLocalUri(
                                unzipDir = unzipDir,
                                mediaFile = media.file,
                            )
                        }
                        val importedCard = Flashcard(
                            id = targetCardId,
                            imageUrl = cardMediaUri.orEmpty(),
                            name = card.text,
                            deckId = targetDeckId,
                            position = targetPosition,
                        )
                        val syncedCard = if (existingCard != null) {
                            flashcardRepository.updateFlashcard(
                                id = targetCardId,
                                newData = importedCard,
                            )
                        } else {
                            flashcardRepository.addFlashcard(importedCard)
                        }
                        if (!syncedCard) {
                            error("Failed to sync card from archive")
                        }
                        importedCardsCount++
                    }

                    if (mode == DeckImportMode.REPLACE_EXISTING) {
                        existingCardsById.keys
                            .filterNot { it in importedCardIds }
                            .forEach { cardId ->
                                val deleted = flashcardRepository.deleteFlashcard(cardId)
                                if (!deleted) {
                                    error("Failed to delete stale card during deck import")
                                }
                            }
                    }

                    val targetStudentId = assignToStudentId?.takeIf { it.isNotBlank() }
                    if (targetStudentId != null) {
                        val alreadyAssigned = studentDeckRepository.getStudentWithDecks(targetStudentId)
                            ?.decks
                            ?.any { deck -> deck.id == targetDeckId } == true
                        if (!alreadyAssigned) {
                            val assigned = studentDeckRepository.assignDeckToStudent(
                                studentId = targetStudentId,
                                deckId = targetDeckId,
                            )
                            if (!assigned) {
                                error("Failed to assign imported deck to student")
                            }
                        }
                    }

                    DeckPackageImportResult(
                        deckId = targetDeckId,
                        deckName = manifest.deck.name,
                        importedCardsCount = importedCardsCount,
                    )
                } catch (error: Throwable) {
                    createdDeckIdForRollback?.let { deckId ->
                        runCatching { deckRepository.deleteDeck(deckId) }
                    }
                    throw error
                }
            } finally {
                sourceArchive.delete()
                unzipDir.deleteRecursively()
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    private fun validateManifest(manifest: DeckPackageManifest) {
        if (manifest.format != RW_DECK_FORMAT) {
            error("Unsupported deck package format: ${manifest.format}")
        }
        if (manifest.schemaVersion != RW_DECK_SCHEMA_VERSION) {
            error("Unsupported deck package schema version: ${manifest.schemaVersion}")
        }
    }

    private fun exportMedia(
        sourceUri: String,
        workDir: File,
        mediaAssets: MutableList<DeckPackageMediaAsset>,
        preferredPrefix: String,
        fallbackKind: DeckPackageMediaKind,
    ): DeckPackageMediaRef? {
        val uri = Uri.parse(sourceUri)
        val extension = resolveExtension(uri, fallback = if (fallbackKind == DeckPackageMediaKind.VIDEO) "mp4" else "jpg")
        val relativePath = "$MEDIA_DIR_NAME/${preferredPrefix}_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 10)}.$extension"
        val outFile = File(workDir, relativePath).apply {
            parentFile?.mkdirs()
        }

        val copied = copyUriToFile(uri, outFile)
        if (!copied) return null

        val kind = detectMediaKind(uri, fallbackKind)
        val sizeBytes = outFile.length()
        val sha = sha256OfFile(outFile)
        mediaAssets += DeckPackageMediaAsset(
            file = relativePath,
            sha256 = sha,
            sizeBytes = sizeBytes,
        )

        return DeckPackageMediaRef(
            kind = kind,
            file = relativePath,
        )
    }

    private fun importMediaAndGetLocalUri(
        unzipDir: File,
        mediaFile: String,
    ): String? {
        val source = safeResolve(unzipDir, mediaFile)
        if (!source.exists() || !source.isFile) return null

        val extension = source.extension.ifBlank { "bin" }
        val mediaRoot = File(appContext.filesDir, "deck_package_media").apply { mkdirs() }
        val target = File(
            mediaRoot,
            "import_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 12)}.$extension",
        )

        source.inputStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return Uri.fromFile(target).toString()
    }

    private fun createWorkDir(prefix: String): File {
        return File(appContext.cacheDir, "$prefix${UniqueIdGenerator.randomAlphanumeric(prefix = "tmp", size = 8)}").apply {
            mkdirs()
        }
    }

    private fun copyArchiveToTemp(archiveUri: String): File {
        val sourceUri = Uri.parse(archiveUri)
        val tempFile = File.createTempFile("rwdeck_", ".zip", appContext.cacheDir)

        openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open archive uri: $archiveUri" }
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun copyUriToFile(
        uri: Uri,
        destination: File,
    ): Boolean {
        return runCatching {
            openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open media uri: $uri" }
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.isSuccess
    }

    private fun openInputStream(uri: Uri): InputStream? {
        val scheme = uri.scheme?.lowercase(Locale.US)
        return when (scheme) {
            "content" -> appContext.contentResolver.openInputStream(uri)
            "file" -> {
                val path = uri.path ?: return null
                FileInputStream(path)
            }
            null -> {
                val rawPath = uri.toString()
                if (rawPath.startsWith("/")) FileInputStream(rawPath) else null
            }
            else -> {
                val maybeFile = File(uri.toString())
                if (maybeFile.exists()) maybeFile.inputStream() else null
            }
        }
    }

    private fun detectMediaKind(
        uri: Uri,
        fallbackKind: DeckPackageMediaKind,
    ): DeckPackageMediaKind {
        val mime = appContext.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("video/")) return DeckPackageMediaKind.VIDEO
        if (mime.startsWith("image/")) return DeckPackageMediaKind.IMAGE

        val ext = resolveExtension(uri, fallback = "").lowercase(Locale.US)
        return if (ext in VIDEO_EXTENSIONS) DeckPackageMediaKind.VIDEO else fallbackKind
    }

    private fun resolveExtension(
        uri: Uri,
        fallback: String,
    ): String {
        val mime = appContext.contentResolver.getType(uri)
        val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        if (!fromMime.isNullOrBlank()) return fromMime

        val fromPath = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
        return fromPath ?: fallback
    }

    private fun sanitizeForFileName(raw: String): String {
        val cleaned = raw
            .trim()
            .replace(Regex("[^a-zA-Z0-9а-яА-Я_-]+"), "_")
            .trim('_')
        return cleaned.ifBlank { "deck" }
    }

    private fun zipDirectory(
        workDir: File,
        outputFile: File,
    ) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
            workDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val entryName = file.relativeTo(workDir).invariantSeparatorsPath
                    zipOut.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
        }
    }

    private fun unzipArchive(
        sourceArchive: File,
        outputDir: File,
    ) {
        ZipInputStream(BufferedInputStream(FileInputStream(sourceArchive))).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val outFile = safeResolve(outputDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { output ->
                        zipIn.copyTo(output)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    private fun safeResolve(rootDir: File, relativePath: String): File {
        val rootCanonical = rootDir.canonicalFile
        val target = File(rootDir, relativePath).canonicalFile
        if (!target.path.startsWith(rootCanonical.path + File.separator)) {
            error("Illegal archive path: $relativePath")
        }
        return target
    }

    private fun sha256OfFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(file.inputStream(), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) >= 0) {
                // DigestInputStream updates digest on read.
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "avi", "mkv", "webm")
    }
}
