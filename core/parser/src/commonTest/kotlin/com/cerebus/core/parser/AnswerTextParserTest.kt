package com.cerebus.core.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerTextParserTest {

    @Test
    fun normalizeParserText_trimsLowercasesAndCollapsesWhitespace() {
        val result = normalizeParserText("  DoG\t \n  CaT  ")

        assertEquals("dog cat", result)
    }

    @Test
    fun parseAnswerVariantsText_splitsByCommonSeparatorsAndDeduplicates() {
        val result = parseAnswerVariantsText(" Dog,cat; DOG |  fish \n cat ")

        assertEquals(listOf("dog", "cat", "fish"), result)
    }

    @Test
    fun parserModule_delegatesToParserFunctions() {
        assertEquals("hello world", ParserModule.normalizeText("  Hello   WORLD  "))
        assertEquals(
            listOf("first", "second"),
            ParserModule.parseAnswerVariants("first, second, FIRST"),
        )
    }
}
