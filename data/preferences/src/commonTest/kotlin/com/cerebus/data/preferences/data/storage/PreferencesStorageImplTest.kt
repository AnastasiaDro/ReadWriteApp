package com.cerebus.data.preferences.data.storage

import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
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

    @Test
    fun saveAndRestoreAllowNeighborTypos_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setAllowNeighborTyposEnabled(
            studentId = "student_1",
            isEnabled = true,
        )

        val restored = storage.getAllowNeighborTyposEnabled("student_1")

        assertEquals(true, restored)
    }

    @Test
    fun allowNeighborTypos_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setAllowNeighborTyposEnabled(
            studentId = "student_1",
            isEnabled = true,
        )

        val restored = storage.getAllowNeighborTyposEnabled("student_2")

        assertNull(restored)
    }

    @Test
    fun saveAndRestoreNeighborTypoSensitivity_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setNeighborTypoSensitivity(
            studentId = "student_1",
            sensitivity = NeighborTypoSensitivity.Soft,
        )

        val restored = storage.getNeighborTypoSensitivity("student_1")

        assertEquals(NeighborTypoSensitivity.Soft, restored)
    }

    @Test
    fun neighborTypoSensitivity_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setNeighborTypoSensitivity(
            studentId = "student_1",
            sensitivity = NeighborTypoSensitivity.Strict,
        )

        val restored = storage.getNeighborTypoSensitivity("student_2")

        assertNull(restored)
    }

    @Test
    fun saveAndRestoreFreeNeighborSlipPresses_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setFreeNeighborSlipPresses(
            studentId = "student_1",
            count = 2,
        )

        val restored = storage.getFreeNeighborSlipPresses("student_1")

        assertEquals(2, restored)
    }

    @Test
    fun freeNeighborSlipPresses_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setFreeNeighborSlipPresses(
            studentId = "student_1",
            count = 1,
        )

        val restored = storage.getFreeNeighborSlipPresses("student_2")

        assertNull(restored)
    }

    @Test
    fun saveAndRestoreKeyboardPressDelay_sameStudent_returnsSavedValue() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardPressDelay(
            studentId = "student_1",
            delay = KeyboardPressDelay.Slow,
        )

        val restored = storage.getKeyboardPressDelay("student_1")

        assertEquals(KeyboardPressDelay.Slow, restored)
    }

    @Test
    fun keyboardPressDelay_otherStudent_returnsNull() {
        val storage = PreferencesStorageImpl(settings = MapSettings())

        storage.setKeyboardPressDelay(
            studentId = "student_1",
            delay = KeyboardPressDelay.Fast,
        )

        val restored = storage.getKeyboardPressDelay("student_2")

        assertNull(restored)
    }
}
