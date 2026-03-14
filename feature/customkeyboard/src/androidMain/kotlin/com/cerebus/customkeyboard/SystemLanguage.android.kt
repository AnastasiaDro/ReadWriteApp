package com.cerebus.customkeyboard

import java.util.Locale

internal actual fun currentSystemLanguageCode(): String {
    return Locale.getDefault().language.lowercase()
}
