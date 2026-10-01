package com.halashasneen.truthtest.monetization

/** Pure retry and error-classification policy; no network request is made by this class. */
object AdRetryPolicy {
    enum class Failure { INTERNAL, CONFIGURATION, NETWORK, NO_FILL, OTHER }

    fun classify(code: Int): Failure = when (code) {
        0 -> Failure.INTERNAL
        1 -> Failure.CONFIGURATION
        2 -> Failure.NETWORK
        3 -> Failure.NO_FILL
        else -> Failure.OTHER
    }

    fun retryDelayMs(failures: Int, failure: Failure): Long = when (failure) {
        Failure.NO_FILL -> (120_000L * failures.coerceIn(1, 3)).coerceAtMost(360_000L)
        Failure.NETWORK -> (30_000L * failures.coerceIn(1, 4)).coerceAtMost(120_000L)
        Failure.CONFIGURATION -> Long.MAX_VALUE // Repeated invalid requests cannot repair an AdMob ID.
        else -> (20_000L * failures.coerceIn(1, 4)).coerceAtMost(80_000L)
    }
}
