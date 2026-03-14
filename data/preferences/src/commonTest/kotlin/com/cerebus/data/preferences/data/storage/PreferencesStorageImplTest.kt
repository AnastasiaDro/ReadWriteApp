package com.cerebus.data.preferences.data.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencesStorageImplTest {

    @Test
    fun saveAndRestoreSnapshot_sameContext_returnsSavedOrder() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1", "deck_2"),
            cardIds = listOf("card_3", "card_1", "card_2"),
        )

        val restored = storage.getLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1", "deck_2"),
        )

        assertContentEquals(listOf("card_3", "card_1", "card_2"), restored)
    }

    @Test
    fun saveAndRestoreSnapshot_deckOrderDoesNotMatter() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_b", "deck_a"),
            cardIds = listOf("card_1", "card_2"),
        )

        val restored = storage.getLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_a", "deck_b"),
        )

        assertContentEquals(listOf("card_1", "card_2"), restored)
    }

    @Test
    fun restoreSnapshot_differentContext_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1"),
            cardIds = listOf("card_1"),
        )

        val restoredWithOtherStudent = storage.getLastSessionCardIds(
            studentId = "student_2",
            deckIds = listOf("deck_1"),
        )
        val restoredWithOtherDeck = storage.getLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_2"),
        )

        assertNull(restoredWithOtherStudent)
        assertNull(restoredWithOtherDeck)
    }

    @Test
    fun saveSnapshot_emptyList_clearsStoredSnapshot() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1"),
            cardIds = listOf("card_1", "card_2"),
        )
        storage.setLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1"),
            cardIds = emptyList(),
        )

        val restored = storage.getLastSessionCardIds(
            studentId = "student_1",
            deckIds = listOf("deck_1"),
        )

        assertNull(restored)
    }

    @Test
    fun saveAndRestoreKeyboardLanguage_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardLanguage(
            studentId = "student_1",
            languageCode = "ru",
        )

        val restored = storage.getKeyboardLanguage("student_1")

        assertEquals("ru", restored)
    }

    @Test
    fun keyboardLanguage_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardLanguage(
            studentId = "student_1",
            languageCode = "en",
        )

        val restored = storage.getKeyboardLanguage("student_2")

        assertNull(restored)
    }

    @Test
    fun saveAndRestoreKeyboardShift_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardShiftEnabled(
            studentId = "student_1",
            isEnabled = true,
        )

        val restored = storage.getKeyboardShiftEnabled("student_1")

        assertTrue(restored == true)
    }

    @Test
    fun keyboardShift_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardShiftEnabled(
            studentId = "student_1",
            isEnabled = true,
        )

        val restored = storage.getKeyboardShiftEnabled("student_2")

        assertNull(restored)
    }

    @Test
    fun saveAndRestorePreventWrongKeyPress_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setPreventWrongKeyPressEnabled(
            studentId = "student_1",
            isEnabled = false,
        )

        val restored = storage.getPreventWrongKeyPressEnabled("student_1")

        assertEquals(false, restored)
    }

    @Test
    fun preventWrongKeyPress_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setPreventWrongKeyPressEnabled(
            studentId = "student_1",
            isEnabled = false,
        )

        val restored = storage.getPreventWrongKeyPressEnabled("student_2")

        assertNull(restored)
    }
}
