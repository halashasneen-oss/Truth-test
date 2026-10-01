package com.halashasneen.truthtest.ui.home

/**
 * Stable product-level sections for Truth Test 2.0.
 *
 * Phase 3 activates the social modes while keeping the same identifiers created
 * in Phase 1 so navigation and local data remain forward-compatible.
 */
enum class ExperienceSection(val stableId: String) {
    TRUTH_TEST("truth_test"),
    PARTY("party"),
    COUPLES("couples"),
    FRIENDS("friends"),
    DAILY_TRUTH("daily_truth"),
    CHALLENGES("challenges"),
    SHARE_STUDIO("share_studio"),
    INSIGHTS("insights"),
    ACHIEVEMENTS("achievements")
}

data class ExperienceDefinition(
    val section: ExperienceSection,
    val offlineCore: Boolean = true,
    val requiresAccount: Boolean = false,
    val legacyCapabilityAvailable: Boolean = false
)

object ExperienceCatalog {
    val ordered: List<ExperienceDefinition> = listOf(
        ExperienceDefinition(ExperienceSection.TRUTH_TEST, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.PARTY, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.COUPLES, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.FRIENDS, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.DAILY_TRUTH, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.CHALLENGES, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.SHARE_STUDIO, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.INSIGHTS, legacyCapabilityAvailable = true),
        ExperienceDefinition(ExperienceSection.ACHIEVEMENTS, legacyCapabilityAvailable = true)
    )

    fun definition(section: ExperienceSection): ExperienceDefinition =
        ordered.first { it.section == section }
}
