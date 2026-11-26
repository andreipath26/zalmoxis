package com.zalmoxis.app
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
class KokoroVoicesTest {
    @Test fun catalogHasAll54Voices() {
        val tags=listOf("en-US","en-GB","es","fr","it","pt-BR","hi","zh-CN","ja")
        val all=tags.flatMap { KokoroVoices.forLocale(Locale.forLanguageTag(it)) }
        assertEquals(54,all.size)
        assertEquals(54,all.toSet().size)
    }
    @Test fun selectedVoiceMustMatchLocale() {
        assertEquals("ff_siwis",KokoroVoices.resolve(Locale.FRENCH,"af_heart"))
        assertEquals("bf_emma",KokoroVoices.resolve(Locale.UK,"bf_emma"))
        assertTrue(KokoroVoices.forLocale(Locale.GERMAN).isEmpty())
    }
}
