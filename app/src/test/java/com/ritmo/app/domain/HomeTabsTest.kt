package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTabsTest {
    @Test fun emptyOrBrokenValuesFallBackToTheDefault() {
        assertEquals(HomeTabs.DEFAULT, HomeTabs.parse(""))
        assertEquals(HomeTabs.DEFAULT, HomeTabs.parse("TODAY"))
        assertEquals(HomeTabs.DEFAULT, HomeTabs.parse("NADA,OUTRA"))
        assertEquals(HomeTabs.DEFAULT, HomeTabs.parse(Screen.entries.joinToString(",") { it.name }))
    }

    @Test fun theOrderIsKeptAndUnknownOrRepeatedNamesAreIgnored() {
        assertEquals(
            listOf(Screen.FOCUS, Screen.TODAY, Screen.SHOP),
            HomeTabs.parse("FOCUS,TODAY,ANTIGA,TODAY,SHOP"),
        )
    }

    @Test fun formatAndParseGoBothWays() {
        val tabs = listOf(Screen.MATRIX, Screen.LISTS, Screen.PROFILE)
        assertEquals(tabs, HomeTabs.parse(HomeTabs.format(tabs)))
    }

    @Test fun togglingRespectsTheLimits() {
        val two = listOf(Screen.TODAY, Screen.LISTS)
        assertNull(HomeTabs.toggle(two, Screen.LISTS))
        assertEquals(two + Screen.FOCUS, HomeTabs.toggle(two, Screen.FOCUS))

        assertNull(HomeTabs.toggle(HomeTabs.DEFAULT, Screen.SHOP))
        assertEquals(HomeTabs.DEFAULT - Screen.MATRIX, HomeTabs.toggle(HomeTabs.DEFAULT, Screen.MATRIX))
    }

    @Test fun movingSwapsNeighboursAndStopsAtTheEnds() {
        val tabs = listOf(Screen.TODAY, Screen.LISTS, Screen.FOCUS)
        assertEquals(listOf(Screen.LISTS, Screen.TODAY, Screen.FOCUS), HomeTabs.move(tabs, Screen.LISTS, -1))
        assertEquals(listOf(Screen.TODAY, Screen.FOCUS, Screen.LISTS), HomeTabs.move(tabs, Screen.LISTS, 1))
        assertEquals(tabs, HomeTabs.move(tabs, Screen.TODAY, -1))
        assertEquals(tabs, HomeTabs.move(tabs, Screen.FOCUS, 1))
        assertEquals(tabs, HomeTabs.move(tabs, Screen.SHOP, 1))
    }
}
