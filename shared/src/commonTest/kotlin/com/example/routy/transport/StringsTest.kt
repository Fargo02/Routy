package com.example.routy.transport

import com.example.routy.core.localization.Strings
import com.example.routy.core.transport.domain.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test fun shortWaitsStayInMinutesAndLongerOnesGainHours() {
        val english = Strings(Language.English)
        assertEquals("0 min", english.minutesShort(0))
        assertEquals("59 min", english.minutesShort(59))
        assertEquals("1 h", english.minutesShort(60))
        assertEquals("1 h 1 min", english.minutesShort(61))
        assertEquals("2 h", english.minutesShort(120))
        assertEquals("23 h 59 min", english.minutesShort(1439))
    }

    @Test fun everyLanguageCarriesItsOwnHourUnit() {
        assertEquals("1 ч 17 мин", Strings(Language.Russian).minutesShort(77))
        assertEquals("1 სთ 17 წთ", Strings(Language.Georgian).minutesShort(77))
        assertEquals("1 h 17 min", Strings(Language.English).minutesShort(77))
    }
}
