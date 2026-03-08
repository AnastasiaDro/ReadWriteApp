package com.cerebus.core.parser

object ParserModule {
    fun normalizeText(value: String): String = normalizeParserText(value)

    fun parseAnswerVariants(raw: String): List<String> = parseAnswerVariantsText(raw)
}
