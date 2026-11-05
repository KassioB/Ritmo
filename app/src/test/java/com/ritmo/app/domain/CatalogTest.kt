package com.ritmo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test fun iconsFromTheFirstVersionStillExist() {
        val original = listOf(
            "💧", "📚", "🏃", "🧘", "🥗", "💊", "🛏️", "✍️", "🎯", "🧹", "🎸", "💻", "🌱", "☀️", "🐶", "🧠",
        )
        val all = IconCatalog.groups.flatMap { it.icons }
        original.forEach { assertTrue("Faltou $it", it in all) }
    }

    @Test fun noIconIsRepeated() {
        val all = IconCatalog.groups.flatMap { it.icons }
        assertEquals(all.toSet().size, all.size)
    }

    @Test fun thereAreManyMoreOptionsThanBefore() {
        assertTrue(IconCatalog.groups.sumOf { it.icons.size } >= 100)
    }

    @Test fun anUnknownIconOpensTheFirstGroup() {
        assertEquals(0, IconCatalog.groupOf("não existe"))
    }

    @Test fun anIconOpensItsOwnGroup() {
        val group = IconCatalog.groups.indexOfFirst { "🛒" in it.icons }
        assertEquals(group, IconCatalog.groupOf("🛒"))
    }
}
