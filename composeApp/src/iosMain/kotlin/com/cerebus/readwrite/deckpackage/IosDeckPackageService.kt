package com.cerebus.readwrite.deckpackage

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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.synth.kmpzip.io.ByteArrayOutputStream
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.zip.ZipOutputStream
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.rewind

private const val MANIFEST_FILE_NAME = "manifest.json"
private const val MEDIA_DIR_NAME = "media"

@OptIn(ExperimentalForeignApi::class)
class IosDeckPackageService(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val studentDeckRepository: StudentDeckRepository,
) : DeckPackageService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun exportDeck(deckId: String): CustomResult<DeckPackageExportFile> = withContext(Dispatchers.Default) {
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

                val manifestPath = safeResolve(workDir, MANIFEST_FILE_NAME)
                writeFileBytes(
                    path = manifestPath,
                    bytes = json.encodeToString(DeckPackageManifest.serializer(), manifest).encodeToByteArray(),
                )

                val outputDir = ensureDirectory("${documentsDirectoryPath()}/deck_packages")
                val safeDeckName = sanitizeForFileName(deck.name)
                val fileName = "${safeDeckName}_${nowMillis()}.rwdeck"
                val outputPath = safeResolve(outputDir, fileName)

                zipDirectory(
                    workDir = workDir,
                    outputFilePath = outputPath,
                )

                DeckPackageExportFile(
                    path = outputPath,
                    fileName = fileName,
                )
            } finally {
                deleteRecursively(workDir)
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    override suspend fun inspectDeckImport(
        archiveUri: String,
    ): CustomResult<DeckPackageImportPreview> = withContext(Dispatchers.Default) {
        runCatching {
            val archiveBytes = readUriBytes(archiveUri)
                ?: error("Unable to read archive uri: $archiveUri")
            val unzipDir = createWorkDir(prefix = "rwdeck_inspect_")

            try {
                unzipArchive(
                    archiveBytes = archiveBytes,
                    outputDir = unzipDir,
                )

                val manifestPath = safeResolve(unzipDir, MANIFEST_FILE_NAME)
                val manifestBytes = readFileBytes(manifestPath)
                    ?: error("manifest.json is missing in archive")
                val manifest = json.decodeFromString(
                    DeckPackageManifest.serializer(),
                    manifestBytes.decodeToString(),
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
                deleteRecursively(unzipDir)
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
    ): CustomResult<DeckPackageImportResult> = withContext(Dispatchers.Default) {
        runCatching {
            val archiveBytes = readUriBytes(archiveUri)
                ?: error("Unable to read archive uri: $archiveUri")
            val unzipDir = createWorkDir(prefix = "rwdeck_import_")

            try {
                unzipArchive(
                    archiveBytes = archiveBytes,
                    outputDir = unzipDir,
                )

                val manifestPath = safeResolve(unzipDir, MANIFEST_FILE_NAME)
                val manifestBytes = readFileBytes(manifestPath)
                    ?: error("manifest.json is missing in archive")
                val manifest = json.decodeFromString(
                    DeckPackageManifest.serializer(),
                    manifestBytes.decodeToString(),
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
                deleteRecursively(unzipDir)
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
        workDir: String,
        mediaAssets: MutableList<DeckPackageMediaAsset>,
        preferredPrefix: String,
        fallbackKind: DeckPackageMediaKind,
    ): DeckPackageMediaRef? {
        val bytes = readUriBytes(sourceUri) ?: return null
        val extension = resolveExtension(
            uri = sourceUri,
            fallback = if (fallbackKind == DeckPackageMediaKind.VIDEO) "mp4" else "jpg",
        )
        val relativePath = "$MEDIA_DIR_NAME/${preferredPrefix}_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 10)}.$extension"
        val outputPath = safeResolve(workDir, relativePath)
        writeFileBytes(outputPath, bytes)

        val kind = detectMediaKind(
            uri = sourceUri,
            fallbackKind = fallbackKind,
        )
        mediaAssets += DeckPackageMediaAsset(
            file = relativePath,
            sizeBytes = bytes.size.toLong(),
        )

        return DeckPackageMediaRef(
            kind = kind,
            file = relativePath,
        )
    }

    private fun importMediaAndGetLocalUri(
        unzipDir: String,
        mediaFile: String,
    ): String? {
        val sourcePath = safeResolve(unzipDir, mediaFile)
        val sourceBytes = readFileBytes(sourcePath) ?: return null

        val extension = sourcePath.substringAfterLast('.', missingDelimiterValue = "bin")
            .ifBlank { "bin" }
        val mediaRoot = ensureDirectory("${documentsDirectoryPath()}/deck_package_media")
        val targetPath = safeResolve(
            mediaRoot,
            "import_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 12)}.$extension",
        )
        writeFileBytes(targetPath, sourceBytes)

        return NSURL.fileURLWithPath(targetPath).absoluteString
    }

    private fun createWorkDir(prefix: String): String {
        val base = normalizePath(NSTemporaryDirectory())
        val path = safeResolve(
            rootDir = base,
            relativePath = "$prefix${UniqueIdGenerator.randomAlphanumeric(prefix = "tmp", size = 8)}",
        )
        ensureDirectory(path)
        return path
    }

    private fun zipDirectory(
        workDir: String,
        outputFilePath: String,
    ) {
        val files = listRelativeFiles(workDir)
        val outputBytes = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(outputBytes)
        try {
            files.forEach { relativePath ->
                val entry = ZipEntry(relativePath)
                zipOut.putNextEntry(entry)
                val filePath = safeResolve(workDir, relativePath)
                val bytes = readFileBytes(filePath) ?: ByteArray(0)
                if (bytes.isNotEmpty()) {
                    zipOut.write(bytes, 0, bytes.size)
                }
                zipOut.closeEntry()
            }
            zipOut.finish()
        } finally {
            zipOut.close()
        }
        writeFileBytes(outputFilePath, outputBytes.toByteArray())
    }

    private fun unzipArchive(
        archiveBytes: ByteArray,
        outputDir: String,
    ) {
        val zipIn = ZipInputStream(archiveBytes)
        try {
            while (true) {
                val entry = zipIn.nextEntry ?: break
                val relativeName = entry.name
                if (relativeName.isBlank()) {
                    zipIn.closeEntry()
                    continue
                }
                val outputPath = safeResolve(outputDir, relativeName)
                if (entry.isDirectory) {
                    ensureDirectory(outputPath)
                } else {
                    ensureDirectory(parentPath(outputPath))
                    val bytes = zipIn.readBytes()
                    writeFileBytes(outputPath, bytes)
                }
                zipIn.closeEntry()
            }
        } finally {
            zipIn.close()
        }
    }

    private fun listRelativeFiles(rootDir: String): List<String> {
        val subpaths = NSFileManager.defaultManager.subpathsOfDirectoryAtPath(
            rootDir,
            error = null,
        ) as? List<*> ?: emptyList<String>()
        return subpaths
            .mapNotNull { it as? String }
            .filter { relativePath ->
                val fullPath = safeResolve(rootDir, relativePath)
                !isDirectory(fullPath)
            }
            .sorted()
    }

    private fun documentsDirectoryPath(): String {
        val documentDir = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String
        return normalizePath(documentDir ?: NSTemporaryDirectory())
    }

    private fun detectMediaKind(
        uri: String,
        fallbackKind: DeckPackageMediaKind,
    ): DeckPackageMediaKind {
        val extension = resolveExtension(uri, fallback = "").lowercase()
        return if (extension in VIDEO_EXTENSIONS) DeckPackageMediaKind.VIDEO else fallbackKind
    }

    private fun resolveExtension(
        uri: String,
        fallback: String,
    ): String {
        val path = resolvePathFromUri(uri) ?: uri
        return path
            .substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun sanitizeForFileName(raw: String): String {
        val cleaned = raw
            .trim()
            .replace(Regex("[^a-zA-Z0-9а-яА-Я_-]+"), "_")
            .trim('_')
        return cleaned.ifBlank { "deck" }
    }

    private fun ensureDirectory(path: String): String {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return path
    }

    private fun deleteRecursively(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    private fun isDirectory(path: String): Boolean {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null) as? Map<Any?, *>
        val fileType = attributes?.get(NSFileType) as? String
        return fileType == NSFileTypeDirectory
    }

    private fun safeResolve(
        rootDir: String,
        relativePath: String,
    ): String {
        val normalizedRoot = normalizePath(rootDir).trimEnd('/')
        val sanitizedRelative = relativePath
            .replace('\\', '/')
            .trim()
            .trimStart('/')
        val combined = normalizePath("$normalizedRoot/$sanitizedRelative")
        if (combined != normalizedRoot && !combined.startsWith("$normalizedRoot/")) {
            error("Illegal archive path: $relativePath")
        }
        return combined
    }

    private fun parentPath(path: String): String {
        val normalized = normalizePath(path)
        val index = normalized.lastIndexOf('/')
        return if (index <= 0) "/" else normalized.substring(0, index)
    }

    private fun normalizePath(path: String): String {
        val sanitized = path.replace('\\', '/')
        val isAbsolute = sanitized.startsWith("/")
        val segments = mutableListOf<String>()
        sanitized.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        val normalized = segments.joinToString("/")
        return if (isAbsolute) "/$normalized" else normalized
    }

    private fun resolvePathFromUri(uri: String): String? {
        if (uri.isBlank()) return null
        if (uri.startsWith("/")) return normalizePath(uri)
        val parsed = NSURL.URLWithString(uri)
        if (parsed?.isFileURL() == true) {
            val path = parsed.path ?: return null
            return normalizePath(path)
        }
        if (uri.startsWith("file://")) {
            val raw = uri.removePrefix("file://")
            if (raw.startsWith("/")) return normalizePath(raw)
        }
        return null
    }

    private fun readUriBytes(uri: String): ByteArray? {
        val path = resolvePathFromUri(uri) ?: return null
        return readFileBytes(path)
    }

    private fun readFileBytes(path: String): ByteArray? {
        val file = fopen(path, "rb") ?: return null
        try {
            if (fseek(file, 0, SEEK_END) != 0) return null
            val sizeLong = ftell(file)
            if (sizeLong < 0) return null
            rewind(file)

            val size = sizeLong.toInt()
            if (size == 0) return ByteArray(0)

            val bytes = ByteArray(size)
            val readCount = bytes.usePinned { pinned ->
                fread(
                    pinned.addressOf(0),
                    1.convert(),
                    size.convert(),
                    file,
                )
            }
            return if (readCount.toInt() == size) bytes else null
        } finally {
            fclose(file)
        }
    }

    private fun writeFileBytes(
        path: String,
        bytes: ByteArray,
    ) {
        ensureDirectory(parentPath(path))
        val file = fopen(path, "wb")
            ?: error("Failed to open file for writing: $path")
        try {
            if (bytes.isEmpty()) return
            val written = bytes.usePinned { pinned ->
                fwrite(
                    pinned.addressOf(0),
                    1.convert(),
                    bytes.size.convert(),
                    file,
                )
            }
            if (written.toInt() != bytes.size) {
                error("Failed to write file: $path")
            }
        } finally {
            fclose(file)
        }
    }

    private companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "avi", "mkv", "webm")
    }
}
