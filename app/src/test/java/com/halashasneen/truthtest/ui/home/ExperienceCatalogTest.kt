package com.halashasneen.truthtest.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceCatalogTest {
    @Test
    fun truthTestTwoSectionsHaveStableUniqueIds() {
        val definitions = ExperienceCatalog.ordered
        assertEquals(9, definitions.size)
        assertEquals(definitions.size, definitions.map { it.section.stableId }.toSet().size)
    }

    @Test
    fun everyCoreSectionIsLocalFirstAndAccountFree() {
        ExperienceCatalog.ordered.forEach { definition ->
            assertTrue(definition.offlineCore)
            assertFalse(definition.requiresAccount)
        }
    }

    @Test
    fun existingCapabilitiesRemainRepresented() {
        val existing = ExperienceCatalog.ordered
            .filter { it.legacyCapabilityAvailable }
            .map { it.section }
            .toSet()
        assertTrue(ExperienceSection.TRUTH_TEST in existing)
        assertTrue(ExperienceSection.PARTY in existing)
        assertTrue(ExperienceSection.DAILY_TRUTH in existing)
        assertTrue(ExperienceSection.SHARE_STUDIO in existing)
        assertTrue(ExperienceSection.INSIGHTS in existing)
        assertTrue(ExperienceSection.ACHIEVEMENTS in existing)
    }
}
