package com.docufind.app.domain.model

enum class InsightPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class InsightTrendPoint(
    val label: String,
    val value: Int
)

data class InsightCategoryCount(
    val label: String,
    val count: Int
)

data class ReminderCompletionSummary(
    val total: Int,
    val completed: Int,
    val active: Int,
    val disabled: Int
)

data class ActivityInsights(
    val period: InsightPeriod,
    val activityTrend: List<InsightTrendPoint>,
    val documentsAddedTrend: List<InsightTrendPoint>,
    val filesStoredTrend: List<InsightTrendPoint>,
    val categoryCounts: List<InsightCategoryCount>,
    val reminderSummary: ReminderCompletionSummary,
    val appOpenedCount: Int,
    val screensVisitedCount: Int,
    val vaultOpenCount: Int,
    val searchUsageCount: Int,
    val sessionDurationMs: Long,
    val filesStoredCount: Int,
    val documentsStoredCount: Int,
    val storageUsageBytes: Long
) {
    companion object {
        fun empty(period: InsightPeriod = InsightPeriod.DAILY) = ActivityInsights(
            period = period,
            activityTrend = emptyList(),
            documentsAddedTrend = emptyList(),
            filesStoredTrend = emptyList(),
            categoryCounts = emptyList(),
            reminderSummary = ReminderCompletionSummary(0, 0, 0, 0),
            appOpenedCount = 0,
            screensVisitedCount = 0,
            vaultOpenCount = 0,
            searchUsageCount = 0,
            sessionDurationMs = 0L,
            filesStoredCount = 0,
            documentsStoredCount = 0,
            storageUsageBytes = 0L
        )
    }
}
