package com.halashasneen.truthtest.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceCatalogTest {
    @Test
    fun sectionIdsAreStableAndUnique() {
        val ids = ExperienceCatalog.ordered.map { it.section.stableId }
        assertEquals(ExperienceSection.entries.size, ExperienceCatalog.ordered.size)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ExperienceSection.TRUTH_TEST, ExperienceCatalog.ordered.first().section)
    }

    @Test
    fun everyCoreSectionIsLocalAndAccountFree() {
        assertTrue(ExperienceCatalog.ordered.all { it.offlineCore })
        assertFalse(ExperienceCatalog.ordered.any { it.requiresAccount })
    }

    @Test
    fun existingCapabilitiesAreMarkedForMigrationIntoNewSections() {
        val available = ExperienceCatalog.ordered
            .filter { it.legacyCapabilityAvailable }
            .map { it.section }
            .toSet()

        assertTrue(ExperienceSection.TRUTH_TEST in available)
        assertTrue(ExperienceSection.PARTY in available)
        assertTrue(ExperienceSection.DAILY_TRUTH in available)
        assertTrue(ExperienceSection.SHARE_STUDIO in available)
        assertTrue(ExperienceSection.INSIGHTS in available)
        assertTrue(ExperienceSection.ACHIEVEMENTS in available)
    }
}
