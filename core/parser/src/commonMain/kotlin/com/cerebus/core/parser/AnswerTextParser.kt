package com.cerebus.core.parser

private val ANSWER_SEPARATOR_REGEX = Regex("[,;|\\n]+")
private val WHITESPACE_REGEX = Regex("\\s+")

fun normalizeParserText(value: String): String {
    return value
        .trim()
        .lowercase()
        .replace(WHITESPACE_REGEX, " ")
}

fun parseAnswerVariantsText(raw: String): List<String> {
    return raw
        .split(ANSWER_SEPARATOR_REGEX)
        .map(::normalizeParserText)
        .filter { it.isNotBlank() }
        .distinct()
}
