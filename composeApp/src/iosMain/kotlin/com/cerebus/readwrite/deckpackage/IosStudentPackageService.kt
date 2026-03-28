package com.cerebus.readwrite.deckpackage

import com.cerebus.core.deck_package.domain.model.RW_STUDENT_FORMAT
import com.cerebus.core.deck_package.domain.model.RW_STUDENT_SCHEMA_VERSION
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaAsset
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaKind
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaRef
import com.cerebus.core.deck_package.domain.model.StudentPackageCardProgress
import com.cerebus.core.deck_package.domain.model.StudentPackageDeck
import com.cerebus.core.deck_package.domain.model.StudentPackageManifest
import com.cerebus.core.deck_package.domain.model.StudentPackageReviewLog
import com.cerebus.core.deck_package.domain.model.StudentPackageSrsPrefs
import com.cerebus.core.deck_package.domain.model.StudentPackageStudent
import com.cerebus.core.deck_package.domain.service.StudentPackageExportFile
import com.cerebus.core.deck_package.domain.service.StudentPackageImportPreview
import com.cerebus.core.deck_package.domain.service.StudentPackageImportResult
import com.cerebus.core.deck_package.domain.service.StudentPackageService
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.student.domain.models.Student
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSTemporaryDirectory
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.rewind

private const val IOS_STUDENT_MANIFEST_FILE_NAME = "manifest.json"
private const val IOS_STUDENT_MEDIA_DIR_NAME = "media"

@OptIn(ExperimentalForeignApi::class)
class IosStudentPackageService(
    private val studentRepository: StudentRepository,
    private val studentDeckRepository: StudentDeckRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
) : StudentPackageService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun exportStudent(studentId: String): CustomResult<StudentPackageExportFile> = withContext(Dispatchers.Default) {
        runCatching {
            val student = studentRepository.getStudentById(studentId)
                ?: error("Student not found: $studentId")
            val studentWithDecks = studentDeckRepository.getStudentWithDecks(studentId)
            val assignedDecks = studentWithDecks?.decks.orEmpty()
            val progressList = cardProgressRepository.observeProgress(studentId).first()
            val reviewLogs = reviewLogRepository.getLogs(studentId)
            val prefs = studentPrefsRepository.getPrefs(studentId)
            val cardsById = flashcardRepository.getFlashcardsByIds(
                (progressList.map { it.cardId } + reviewLogs.map { it.cardId }).distinct()
            )
                .associateBy { it.id }
            val progressByDeckId = progressList.groupBy { progress ->
                cardsById[progress.cardId]?.deckId
            }
            val reviewLogsByDeckId = reviewLogs.groupBy { log ->
                cardsById[log.cardId]?.deckId
            }

            val workDir = createWorkDir(prefix = "rwstudent_export_")
            try {
                val mediaAssets = mutableListOf<DeckPackageMediaAsset>()
                val avatarMedia = student.avatarUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { sourceUri ->
                        exportMedia(
                            sourceUri = sourceUri,
                            workDir = workDir,
                            mediaAssets = mediaAssets,
                            preferredPrefix = "student_avatar",
                            fallbackKind = DeckPackageMediaKind.IMAGE,
                        )
                    }

                val manifest = StudentPackageManifest(
                    exportedAtEpochMillis = nowMillis(),
                    student = StudentPackageStudent(
                        sourceStudentId = student.id,
                        name = student.name,
                        avatarMedia = avatarMedia,
                        activeLetters = student.activeLetters,
                        srsPrefs = prefs.toPackageModel(),
                    ),
                    decks = assignedDecks.map { deck ->
                        StudentPackageDeck(
                            sourceDeckId = deck.id,
                            deckName = deck.name,
                            cards = progressByDeckId[deck.id].orEmpty()
                                .mapNotNull { progress ->
                                    val card = cardsById[progress.cardId] ?: return@mapNotNull null
                                    progress.toPackageModel(cardName = card.name)
                                },
                            reviewLogs = reviewLogsByDeckId[deck.id].orEmpty()
                                .mapNotNull { reviewLog ->
                                    val card = cardsById[reviewLog.cardId] ?: return@mapNotNull null
                                    reviewLog.toPackageModel(cardName = card.name)
                                },
                        )
                    },
                    media = mediaAssets,
                )

                val manifestPath = safeResolve(workDir, IOS_STUDENT_MANIFEST_FILE_NAME)
                writeFileBytes(
                    path = manifestPath,
                    bytes = json.encodeToString(StudentPackageManifest.serializer(), manifest).encodeToByteArray(),
                )

                val outputDir = ensureDirectory("${documentsDirectoryPath()}/student_packages")
                val safeStudentName = sanitizeForFileName(student.name.ifBlank { "student" })
                val fileName = "${safeStudentName}_${nowMillis()}.rwstudent"
                val outputPath = safeResolve(outputDir, fileName)

                zipDirectory(workDir = workDir, outputFilePath = outputPath)

                StudentPackageExportFile(
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

    override suspend fun inspectStudentImport(
        archiveUri: String,
    ): CustomResult<StudentPackageImportPreview> = withContext(Dispatchers.Default) {
        runCatching {
            val archiveBytes = readUriBytes(archiveUri)
                ?: error("Unable to read archive uri: $archiveUri")
            val unzipDir = createWorkDir(prefix = "rwstudent_inspect_")

            try {
                unzipArchive(archiveBytes = archiveBytes, outputDir = unzipDir)

                val manifestPath = safeResolve(unzipDir, IOS_STUDENT_MANIFEST_FILE_NAME)
                val manifestBytes = readFileBytes(manifestPath)
                    ?: error("manifest.json is missing in archive")
                val manifest = json.decodeFromString(
                    StudentPackageManifest.serializer(),
                    manifestBytes.decodeToString(),
                )
                validateManifest(manifest)

                val existingStudent = manifest.student.sourceStudentId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { studentId -> studentRepository.getStudentById(studentId) }
                val allDecks = deckRepository.observeAllDecks().first()
                val stats = collectImportStats(
                    manifest = manifest,
                    allDecks = allDecks,
                )

                StudentPackageImportPreview(
                    studentName = manifest.student.name,
                    existingStudentId = existingStudent?.id,
                    existingStudentName = existingStudent?.name,
                    matchedDecksCount = stats.matchedDecksCount,
                    missingDecksCount = stats.missingDecksCount,
                    matchedCardsCount = stats.matchedCardsCount,
                    missingCardsCount = stats.missingCardsCount,
                )
            } finally {
                deleteRecursively(unzipDir)
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    override suspend fun importStudent(
        archiveUri: String,
    ): CustomResult<StudentPackageImportResult> = withContext(Dispatchers.Default) {
        runCatching {
            val archiveBytes = readUriBytes(archiveUri)
                ?: error("Unable to read archive uri: $archiveUri")
            val unzipDir = createWorkDir(prefix = "rwstudent_import_")

            try {
                unzipArchive(archiveBytes = archiveBytes, outputDir = unzipDir)

                val manifestPath = safeResolve(unzipDir, IOS_STUDENT_MANIFEST_FILE_NAME)
                val manifestBytes = readFileBytes(manifestPath)
                    ?: error("manifest.json is missing in archive")
                val manifest = json.decodeFromString(
                    StudentPackageManifest.serializer(),
                    manifestBytes.decodeToString(),
                )
                validateManifest(manifest)

                val sourceStudentId = manifest.student.sourceStudentId?.takeIf { it.isNotBlank() }
                val existingStudent = sourceStudentId?.let { studentRepository.getStudentById(it) }
                val targetStudentId = existingStudent?.id
                    ?: sourceStudentId
                    ?: UniqueIdGenerator.randomAlphanumeric(prefix = "student")
                val avatarUri = manifest.student.avatarMedia?.let { media ->
                    importMediaAndGetLocalUri(
                        unzipDir = unzipDir,
                        mediaFile = media.file,
                    )
                }

                if (existingStudent == null) {
                    val created = studentRepository.createStudent(
                        Student(
                            id = targetStudentId,
                            name = manifest.student.name,
                            avatarUri = avatarUri,
                            activeLetters = manifest.student.activeLetters,
                        )
                    )
                    if (!created) {
                        error("Failed to create student from archive")
                    }
                } else {
                    val updatedName = studentRepository.updateName(
                        id = targetStudentId,
                        newName = manifest.student.name,
                    )
                    val updatedLetters = studentRepository.updateActiveLetters(
                        id = targetStudentId,
                        activeLetters = manifest.student.activeLetters,
                    )
                    val updatedAvatar = manifest.student.avatarMedia == null || studentRepository.updateAvatarUri(
                        id = targetStudentId,
                        avatarUri = avatarUri,
                    )
                    if (!updatedName || !updatedLetters || !updatedAvatar) {
                        error("Failed to update student from archive")
                    }
                }

                val mergedPrefs = mergeImportedPrefs(
                    existing = studentPrefsRepository.getPrefs(targetStudentId),
                    imported = manifest.student.srsPrefs.toDomain(studentId = targetStudentId),
                )
                studentPrefsRepository.savePrefs(mergedPrefs)

                val allDecks = deckRepository.observeAllDecks().first()
                val allDecksById = allDecks.associateBy { deck -> deck.id }
                val allDecksByName = allDecks.associateBy { deck -> normalizeName(deck.name) }
                val existingReviewLogFingerprints = reviewLogRepository.getLogs(targetStudentId)
                    .mapTo(mutableSetOf()) { reviewLog -> ReviewLogFingerprint.fromDomain(reviewLog) }
                var matchedDecksCount = 0
                var restoredCardsCount = 0
                var skippedDecksCount = 0
                var skippedCardsCount = 0

                manifest.decks.forEach { deckEntry ->
                    val matchedDeck = findMatchingDeck(
                        deckEntry = deckEntry,
                        decksById = allDecksById,
                        decksByName = allDecksByName,
                    )
                    if (matchedDeck == null) {
                        skippedDecksCount++
                        skippedCardsCount += deckEntry.cards.size
                        return@forEach
                    }

                    matchedDecksCount++
                    ensureDeckAssigned(
                        studentId = targetStudentId,
                        deckId = matchedDeck.id,
                    )

                    val deckCards = flashcardRepository.getFlashcardsByDeckId(matchedDeck.id)
                    val cardsById = deckCards.associateBy { card -> card.id }
                    val cardsByName = deckCards.associateBy { card -> normalizeName(card.name) }

                    deckEntry.cards.forEach { progressEntry ->
                        val matchedCard = findMatchingCard(
                            progressEntry = progressEntry,
                            cardsById = cardsById,
                            cardsByName = cardsByName,
                        )
                        if (matchedCard == null) {
                            skippedCardsCount++
                            return@forEach
                        }

                        val importedProgress = progressEntry.toDomain(
                            studentId = targetStudentId,
                            cardId = matchedCard.id,
                        )
                        val existingProgress = cardProgressRepository.getProgress(
                            studentId = targetStudentId,
                            cardId = matchedCard.id,
                        )
                        cardProgressRepository.upsertProgress(
                            mergeImportedProgress(
                                existing = existingProgress,
                                imported = importedProgress,
                            )
                        )
                        restoredCardsCount++
                    }

                    deckEntry.reviewLogs.forEach { reviewLogEntry ->
                        val matchedCard = findMatchingCard(
                            sourceCardId = reviewLogEntry.sourceCardId,
                            cardName = reviewLogEntry.cardName,
                            cardsById = cardsById,
                            cardsByName = cardsByName,
                        )
                        if (matchedCard == null) return@forEach

                        val reviewLog = reviewLogEntry.toDomain(
                            studentId = targetStudentId,
                            cardId = matchedCard.id,
                        )
                        val fingerprint = ReviewLogFingerprint.fromDomain(reviewLog)
                        if (!existingReviewLogFingerprints.add(fingerprint)) return@forEach
                        reviewLogRepository.insertLog(reviewLog)
                    }
                }

                StudentPackageImportResult(
                    studentId = targetStudentId,
                    studentName = manifest.student.name,
                    matchedDecksCount = matchedDecksCount,
                    restoredCardsCount = restoredCardsCount,
                    skippedDecksCount = skippedDecksCount,
                    skippedCardsCount = skippedCardsCount,
                    updatedExistingStudent = existingStudent != null,
                )
            } finally {
                deleteRecursively(unzipDir)
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    private suspend fun ensureDeckAssigned(
        studentId: String,
        deckId: String,
    ) {
        val alreadyAssigned = studentDeckRepository.getStudentWithDecks(studentId)
            ?.decks
            ?.any { deck -> deck.id == deckId } == true
        if (alreadyAssigned) return
        val assigned = studentDeckRepository.assignDeckToStudent(
            studentId = studentId,
            deckId = deckId,
        )
        if (!assigned) {
            error("Failed to assign imported deck to student")
        }
    }

    private suspend fun collectImportStats(
        manifest: StudentPackageManifest,
        allDecks: List<Deck>,
    ): StudentImportStats {
        val allDecksById = allDecks.associateBy { deck -> deck.id }
        val allDecksByName = allDecks.associateBy { deck -> normalizeName(deck.name) }
        var matchedDecksCount = 0
        var missingDecksCount = 0
        var matchedCardsCount = 0
        var missingCardsCount = 0

        manifest.decks.forEach { deckEntry ->
            val matchedDeck = findMatchingDeck(
                deckEntry = deckEntry,
                decksById = allDecksById,
                decksByName = allDecksByName,
            )
            if (matchedDeck == null) {
                missingDecksCount++
                missingCardsCount += deckEntry.cards.size
                return@forEach
            }

            matchedDecksCount++
            val deckCards = flashcardRepository.getFlashcardsByDeckId(matchedDeck.id)
            val cardsById = deckCards.associateBy { card -> card.id }
            val cardsByName = deckCards.associateBy { card -> normalizeName(card.name) }

            deckEntry.cards.forEach { progressEntry ->
                val matchedCard = findMatchingCard(
                    progressEntry = progressEntry,
                    cardsById = cardsById,
                    cardsByName = cardsByName,
                )
                if (matchedCard == null) {
                    missingCardsCount++
                } else {
                    matchedCardsCount++
                }
            }
        }

        return StudentImportStats(
            matchedDecksCount = matchedDecksCount,
            missingDecksCount = missingDecksCount,
            matchedCardsCount = matchedCardsCount,
            missingCardsCount = missingCardsCount,
        )
    }

    private fun findMatchingDeck(
        deckEntry: StudentPackageDeck,
        decksById: Map<String, Deck>,
        decksByName: Map<String, Deck>,
    ): Deck? {
        val byId = deckEntry.sourceDeckId
            ?.takeIf { it.isNotBlank() }
            ?.let { deckId -> decksById[deckId] }
        if (byId != null) return byId
        return decksByName[normalizeName(deckEntry.deckName)]
    }

    private fun findMatchingCard(
        progressEntry: StudentPackageCardProgress,
        cardsById: Map<String, Flashcard>,
        cardsByName: Map<String, Flashcard>,
    ): Flashcard? {
        return findMatchingCard(
            sourceCardId = progressEntry.sourceCardId,
            cardName = progressEntry.cardName,
            cardsById = cardsById,
            cardsByName = cardsByName,
        )
    }

    private fun findMatchingCard(
        sourceCardId: String?,
        cardName: String,
        cardsById: Map<String, Flashcard>,
        cardsByName: Map<String, Flashcard>,
    ): Flashcard? {
        val byId = sourceCardId
            ?.takeIf { it.isNotBlank() }
            ?.let { cardId -> cardsById[cardId] }
        if (byId != null) return byId
        return cardsByName[normalizeName(cardName)]
    }

    private fun validateManifest(manifest: StudentPackageManifest) {
        if (manifest.format != RW_STUDENT_FORMAT) {
            error("Unsupported student package format: ${manifest.format}")
        }
        if (manifest.schemaVersion != RW_STUDENT_SCHEMA_VERSION) {
            error("Unsupported student package schema version: ${manifest.schemaVersion}")
        }
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
        val relativePath = "$IOS_STUDENT_MEDIA_DIR_NAME/${preferredPrefix}_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 10)}.$extension"
        val outputPath = safeResolve(workDir, relativePath)
        writeFileBytes(outputPath, bytes)

        mediaAssets += DeckPackageMediaAsset(
            file = relativePath,
            sizeBytes = bytes.size.toLong(),
        )

        return DeckPackageMediaRef(
            kind = detectMediaKind(
                uri = sourceUri,
                fallbackKind = fallbackKind,
            ),
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
        val mediaRoot = ensureDirectory("${documentsDirectoryPath()}/student_package_media")
        val targetPath = safeResolve(
            mediaRoot,
            "import_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 12)}.$extension",
        )
        writeFileBytes(targetPath, sourceBytes)

        return NSURL.fileURLWithPath(targetPath).absoluteString
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

    private fun sanitizeForFileName(raw: String): String {
        val cleaned = raw
            .trim()
            .replace(Regex("[^a-zA-Z0-9а-яА-Я_-]+"), "_")
            .trim('_')
        return cleaned.ifBlank { "student" }
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

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }
}

private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "webm", "mkv")

private data class StudentImportStats(
    val matchedDecksCount: Int,
    val missingDecksCount: Int,
    val matchedCardsCount: Int,
    val missingCardsCount: Int,
)

private fun StudentSrsPrefs.toPackageModel(): StudentPackageSrsPrefs {
    return StudentPackageSrsPrefs(
        newCardsPerSession = newCardsPerSession,
        reviewsPerSession = reviewsPerSession,
        learnMoreStep = learnMoreStep,
        maxNewCardsPerDay = maxNewCardsPerDay,
        allowNearMatch = allowNearMatch,
        similarityThreshold = similarityThreshold,
        easyStreakRequired = easyStreakRequired,
        guidedHintSuccessThreshold = guidedHintSuccessThreshold,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

private fun StudentPackageSrsPrefs.toDomain(studentId: String): StudentSrsPrefs {
    return StudentSrsPrefs(
        studentId = studentId,
        newCardsPerSession = newCardsPerSession,
        reviewsPerSession = reviewsPerSession,
        learnMoreStep = learnMoreStep,
        maxNewCardsPerDay = maxNewCardsPerDay,
        allowNearMatch = allowNearMatch,
        similarityThreshold = similarityThreshold,
        easyStreakRequired = easyStreakRequired,
        guidedHintSuccessThreshold = guidedHintSuccessThreshold,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

private fun mergeImportedPrefs(
    existing: StudentSrsPrefs,
    imported: StudentSrsPrefs,
): StudentSrsPrefs {
    val existingUpdatedAt = existing.updatedAtEpochMillis
    val importedUpdatedAt = imported.updatedAtEpochMillis

    return when {
        importedUpdatedAt > existingUpdatedAt -> imported
        existingUpdatedAt > importedUpdatedAt -> existing
        else -> imported
    }
}

private fun CardProgress.toPackageModel(cardName: String): StudentPackageCardProgress {
    return StudentPackageCardProgress(
        sourceCardId = cardId,
        cardName = cardName,
        level = level,
        dueAtEpochMillis = dueAtEpochMillis,
        recallSuccessStreak = recallSuccessStreak,
        copySuccessStreak = copySuccessStreak,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastHintLevel = lastHintLevel,
        lastDurationMs = lastDurationMs,
        lastWrongPressCount = lastWrongPressCount,
    )
}

private fun ReviewLog.toPackageModel(cardName: String): StudentPackageReviewLog {
    return StudentPackageReviewLog(
        sourceCardId = cardId,
        cardName = cardName,
        shownAtEpochMillis = shownAtEpochMillis,
        submittedAtEpochMillis = submittedAtEpochMillis,
        userInputRaw = userInputRaw,
        userInputNormalized = userInputNormalized,
        expectedAnswerNormalized = expectedAnswerNormalized,
        isCorrect = isCorrect,
        hintLevel = hintLevel,
        wrongPressCount = wrongPressCount,
        durationMs = durationMs,
        copyStage = copyStage,
        levelBefore = levelBefore,
        levelAfter = levelAfter,
        recallSuccessStreakBefore = recallSuccessStreakBefore,
        recallSuccessStreakAfter = recallSuccessStreakAfter,
        copySuccessStreakBefore = copySuccessStreakBefore,
        copySuccessStreakAfter = copySuccessStreakAfter,
        dueAtBeforeEpochMillis = dueAtBeforeEpochMillis,
        dueAtAfterEpochMillis = dueAtAfterEpochMillis,
    )
}

private fun StudentPackageCardProgress.toDomain(
    studentId: String,
    cardId: String,
): CardProgress {
    return CardProgress(
        studentId = studentId,
        cardId = cardId,
        level = level,
        dueAtEpochMillis = dueAtEpochMillis,
        recallSuccessStreak = recallSuccessStreak,
        copySuccessStreak = copySuccessStreak,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastHintLevel = lastHintLevel,
        lastDurationMs = lastDurationMs,
        lastWrongPressCount = lastWrongPressCount,
    )
}

private fun StudentPackageReviewLog.toDomain(
    studentId: String,
    cardId: String,
): ReviewLog {
    return ReviewLog(
        studentId = studentId,
        cardId = cardId,
        shownAtEpochMillis = shownAtEpochMillis,
        submittedAtEpochMillis = submittedAtEpochMillis,
        userInputRaw = userInputRaw,
        userInputNormalized = userInputNormalized,
        expectedAnswerNormalized = expectedAnswerNormalized,
        isCorrect = isCorrect,
        hintLevel = hintLevel,
        wrongPressCount = wrongPressCount,
        durationMs = durationMs,
        copyStage = copyStage,
        levelBefore = levelBefore,
        levelAfter = levelAfter,
        recallSuccessStreakBefore = recallSuccessStreakBefore,
        recallSuccessStreakAfter = recallSuccessStreakAfter,
        copySuccessStreakBefore = copySuccessStreakBefore,
        copySuccessStreakAfter = copySuccessStreakAfter,
        dueAtBeforeEpochMillis = dueAtBeforeEpochMillis,
        dueAtAfterEpochMillis = dueAtAfterEpochMillis,
    )
}

private fun mergeImportedProgress(
    existing: CardProgress?,
    imported: CardProgress,
): CardProgress {
    if (existing == null) return imported

    val existingReviewedAt = existing.lastReviewedAtEpochMillis ?: Long.MIN_VALUE
    val importedReviewedAt = imported.lastReviewedAtEpochMillis ?: Long.MIN_VALUE

    return when {
        importedReviewedAt > existingReviewedAt -> imported
        existingReviewedAt > importedReviewedAt -> existing
        imported.level > existing.level -> imported
        existing.level > imported.level -> existing
        imported.dueAtEpochMillis > existing.dueAtEpochMillis -> imported
        existing.dueAtEpochMillis > imported.dueAtEpochMillis -> existing
        imported.recallSuccessStreak + imported.copySuccessStreak >=
            existing.recallSuccessStreak + existing.copySuccessStreak -> imported
        else -> existing
    }
}

private data class ReviewLogFingerprint(
    val cardId: String,
    val shownAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val userInputRaw: String,
    val userInputNormalized: String,
    val expectedAnswerNormalized: String,
    val isCorrect: Boolean,
    val hintLevel: Int,
    val wrongPressCount: Int,
    val durationMs: Long,
    val copyStage: Boolean,
    val levelBefore: Int,
    val levelAfter: Int,
    val recallSuccessStreakBefore: Int,
    val recallSuccessStreakAfter: Int,
    val copySuccessStreakBefore: Int,
    val copySuccessStreakAfter: Int,
    val dueAtBeforeEpochMillis: Long,
    val dueAtAfterEpochMillis: Long,
) {
    companion object {
        fun fromDomain(reviewLog: ReviewLog): ReviewLogFingerprint {
            return ReviewLogFingerprint(
                cardId = reviewLog.cardId,
                shownAtEpochMillis = reviewLog.shownAtEpochMillis,
                submittedAtEpochMillis = reviewLog.submittedAtEpochMillis,
                userInputRaw = reviewLog.userInputRaw,
                userInputNormalized = reviewLog.userInputNormalized,
                expectedAnswerNormalized = reviewLog.expectedAnswerNormalized,
                isCorrect = reviewLog.isCorrect,
                hintLevel = reviewLog.hintLevel,
                wrongPressCount = reviewLog.wrongPressCount,
                durationMs = reviewLog.durationMs,
                copyStage = reviewLog.copyStage,
                levelBefore = reviewLog.levelBefore,
                levelAfter = reviewLog.levelAfter,
                recallSuccessStreakBefore = reviewLog.recallSuccessStreakBefore,
                recallSuccessStreakAfter = reviewLog.recallSuccessStreakAfter,
                copySuccessStreakBefore = reviewLog.copySuccessStreakBefore,
                copySuccessStreakAfter = reviewLog.copySuccessStreakAfter,
                dueAtBeforeEpochMillis = reviewLog.dueAtBeforeEpochMillis,
                dueAtAfterEpochMillis = reviewLog.dueAtAfterEpochMillis,
            )
        }
    }
}
