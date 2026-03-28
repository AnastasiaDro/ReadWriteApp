package com.cerebus.readwrite.deckpackage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaAsset
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaKind
import com.cerebus.core.deck_package.domain.model.DeckPackageMediaRef
import com.cerebus.core.deck_package.domain.model.RW_STUDENT_FORMAT
import com.cerebus.core.deck_package.domain.model.RW_STUDENT_SCHEMA_VERSION
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val STUDENT_MANIFEST_FILE_NAME = "manifest.json"
private const val STUDENT_MEDIA_DIR_NAME = "media"

class AndroidStudentPackageService(
    private val appContext: Context,
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

    override suspend fun exportStudent(studentId: String): CustomResult<StudentPackageExportFile> = withContext(Dispatchers.IO) {
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
                    student = addManifestStudent(
                        student = student,
                        prefs = prefs,
                        avatarMedia = avatarMedia,
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

                val manifestFile = File(workDir, STUDENT_MANIFEST_FILE_NAME)
                manifestFile.writeText(json.encodeToString(StudentPackageManifest.serializer(), manifest))

                val preferredDownloadRoot = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val outputDir = File(preferredDownloadRoot ?: appContext.filesDir, "student_packages").apply { mkdirs() }
                val safeStudentName = sanitizeForFileName(student.name.ifBlank { "student" })
                val fileName = "${safeStudentName}_${nowMillis()}.rwstudent"
                val outputFile = File(outputDir, fileName)

                zipDirectory(workDir = workDir, outputFile = outputFile)

                StudentPackageExportFile(
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

    override suspend fun inspectStudentImport(
        archiveUri: String,
    ): CustomResult<StudentPackageImportPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceArchive = copyArchiveToTemp(archiveUri)
            val unzipDir = createWorkDir(prefix = "rwstudent_inspect_")

            try {
                unzipArchive(sourceArchive, unzipDir)

                val manifestFile = File(unzipDir, STUDENT_MANIFEST_FILE_NAME)
                if (!manifestFile.exists()) {
                    error("manifest.json is missing in archive")
                }
                val manifest = json.decodeFromString(
                    StudentPackageManifest.serializer(),
                    manifestFile.readText(),
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
                sourceArchive.delete()
                unzipDir.deleteRecursively()
            }
        }.fold(
            onSuccess = { CustomResult.Success(it) },
            onFailure = { CustomResult.Failure(it) },
        )
    }

    override suspend fun importStudent(
        archiveUri: String,
    ): CustomResult<StudentPackageImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceArchive = copyArchiveToTemp(archiveUri)
            val unzipDir = createWorkDir(prefix = "rwstudent_import_")

            try {
                unzipArchive(sourceArchive, unzipDir)

                val manifestFile = File(unzipDir, STUDENT_MANIFEST_FILE_NAME)
                if (!manifestFile.exists()) {
                    error("manifest.json is missing in archive")
                }
                val manifest = json.decodeFromString(
                    StudentPackageManifest.serializer(),
                    manifestFile.readText(),
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
                val allDecksByName = allDecks.associateBy { deck -> normalizeName(it = deck.name) }
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
                sourceArchive.delete()
                unzipDir.deleteRecursively()
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

    private fun createWorkDir(prefix: String): File {
        return File(appContext.cacheDir, "$prefix${UniqueIdGenerator.randomAlphanumeric(prefix = "tmp", size = 8)}").apply {
            mkdirs()
        }
    }

    private fun addManifestStudent(
        student: Student,
        prefs: StudentSrsPrefs,
        avatarMedia: DeckPackageMediaRef?,
    ): StudentPackageStudent {
        return StudentPackageStudent(
            sourceStudentId = student.id,
            name = student.name,
            avatarMedia = avatarMedia,
            activeLetters = student.activeLetters,
            srsPrefs = prefs.toPackageModel(),
        )
    }

    private fun copyArchiveToTemp(archiveUri: String): File {
        val sourceUri = Uri.parse(archiveUri)
        val tempFile = File.createTempFile("rwstudent_", ".zip", appContext.cacheDir)

        openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open archive uri: $archiveUri" }
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
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
        val relativePath = "$STUDENT_MEDIA_DIR_NAME/${preferredPrefix}_${UniqueIdGenerator.randomAlphanumeric(prefix = "m", size = 10)}.$extension"
        val outFile = File(workDir, relativePath).apply {
            parentFile?.mkdirs()
        }

        val copied = copyUriToFile(uri, outFile)
        if (!copied) return null

        val kind = detectMediaKind(uri, fallbackKind)
        mediaAssets += DeckPackageMediaAsset(
            file = relativePath,
            sha256 = sha256OfFile(outFile),
            sizeBytes = outFile.length(),
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
        val mediaRoot = File(appContext.filesDir, "student_package_media").apply { mkdirs() }
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
        return cleaned.ifBlank { "student" }
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

    private fun normalizeName(it: String): String {
        return it.trim().lowercase(Locale.US)
    }

    private fun sha256OfFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(file.inputStream(), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) != -1) {
                // Read through the whole stream to update the digest.
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
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
