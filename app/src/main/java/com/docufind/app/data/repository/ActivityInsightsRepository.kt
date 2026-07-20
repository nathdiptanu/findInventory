package com.docufind.app.data.repository

import com.docufind.app.data.local.db.dao.ActivityEventDao
import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.dao.VaultFileDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.data.local.db.entity.ActivityEvent
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.data.local.db.entity.VaultFile
import com.docufind.app.data.local.db.entity.VaultRecord
import com.docufind.app.domain.model.ActivityInsights
import com.docufind.app.domain.model.InsightCategoryCount
import com.docufind.app.domain.model.InsightPeriod
import com.docufind.app.domain.model.InsightTrendPoint
import com.docufind.app.domain.model.ReminderCompletionSummary
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.insights.ActivityEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityInsightsRepository @Inject constructor(
    private val activityEventDao: ActivityEventDao,
    private val vaultRecordDao: VaultRecordDao,
    private val vaultFileDao: VaultFileDao,
    private val reminderDao: ReminderDao
) {
    fun observeInsights(period: InsightPeriod): Flow<ActivityInsights> {
        val since = LocalDate.now().minusYears(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return combine(
            activityEventDao.observeSince(since),
            vaultRecordDao.observeAllForInsights(),
            vaultFileDao.observeAllForInsights(),
            reminderDao.observeAll()
        ) { events, records, files, reminders ->
            buildInsights(period, events, records, files, reminders)
        }.flowOn(Dispatchers.Default)
    }

    private fun buildInsights(
        period: InsightPeriod,
        events: List<ActivityEvent>,
        records: List<VaultRecord>,
        files: List<VaultFile>,
        reminders: List<Reminder>
    ): ActivityInsights {
        val buckets = periodBuckets(period)
        val activityEvents = events.filter {
            it.type != ActivityEventType.SESSION_STARTED.name &&
                it.type != ActivityEventType.SESSION_ENDED.name
        }

        val categoryCounts = records
            .groupingBy { it.category }
            .eachCount()
            .map { (category, count) ->
                InsightCategoryCount(
                    label = DocuFindModule.fromId(category)?.title ?: category.replace('_', ' '),
                    count = count
                )
            }
            .sortedWith(compareByDescending<InsightCategoryCount> { it.count }.thenBy { it.label })

        val totalDurationMs = events
            .filter { it.type == ActivityEventType.SESSION_ENDED.name }
            .sumOf { it.durationMs ?: 0L }

        return ActivityInsights(
            period = period,
            activityTrend = trendFrom(buckets, activityEvents.map { it.timestamp }),
            documentsAddedTrend = trendFrom(buckets, records.map { it.createdAt }),
            filesStoredTrend = trendFrom(buckets, files.map { it.createdAt }),
            categoryCounts = categoryCounts,
            reminderSummary = reminderSummary(reminders),
            appOpenedCount = events.count { it.type == ActivityEventType.APP_OPENED.name },
            screensVisitedCount = events.count { it.type == ActivityEventType.SCREEN_VIEW.name },
            vaultOpenCount = events.count { it.type == ActivityEventType.VAULT_OPENED.name },
            searchUsageCount = events.count { it.type == ActivityEventType.SEARCH_USED.name },
            sessionDurationMs = totalDurationMs,
            filesStoredCount = files.size,
            documentsStoredCount = records.size,
            storageUsageBytes = files.sumOf { it.fileSize.coerceAtLeast(0L) }
        )
    }

    private fun reminderSummary(reminders: List<Reminder>): ReminderCompletionSummary {
        val completed = reminders.count {
            ReminderStatus.fromStored(it.status) == ReminderStatus.COMPLETED
        }
        val active = reminders.count {
            ReminderStatus.fromStored(it.status) == ReminderStatus.ACTIVE
        }
        val disabled = reminders.count {
            ReminderStatus.fromStored(it.status) == ReminderStatus.DISABLED
        }
        return ReminderCompletionSummary(
            total = reminders.size,
            completed = completed,
            active = active,
            disabled = disabled
        )
    }

    private fun trendFrom(
        buckets: List<TimeBucket>,
        timestamps: List<Long>
    ): List<InsightTrendPoint> {
        val counts = timestamps.groupingBy { bucketKey(it, buckets.first().period) }.eachCount()
        return buckets.map { bucket ->
            InsightTrendPoint(bucket.label, counts[bucket.key] ?: 0)
        }
    }

    private fun periodBuckets(period: InsightPeriod): List<TimeBucket> {
        val today = LocalDate.now()
        return when (period) {
            InsightPeriod.DAILY -> (6 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                TimeBucket(
                    key = day.toString(),
                    label = day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                    period = period
                )
            }
            InsightPeriod.WEEKLY -> (7 downTo 0).map { offset ->
                val day = today.minusWeeks(offset.toLong())
                val weekFields = WeekFields.of(Locale.getDefault())
                TimeBucket(
                    key = "${day.get(weekFields.weekBasedYear())}-W${day.get(weekFields.weekOfWeekBasedYear())}",
                    label = "W${day.get(weekFields.weekOfWeekBasedYear())}",
                    period = period
                )
            }
            InsightPeriod.MONTHLY -> (5 downTo 0).map { offset ->
                val month = YearMonth.from(today.minusMonths(offset.toLong()))
                TimeBucket(
                    key = month.toString(),
                    label = month.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())),
                    period = period
                )
            }
        }
    }

    private fun bucketKey(timestamp: Long, period: InsightPeriod): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (period) {
            InsightPeriod.DAILY -> date.toString()
            InsightPeriod.WEEKLY -> {
                val weekFields = WeekFields.of(Locale.getDefault())
                "${date.get(weekFields.weekBasedYear())}-W${date.get(weekFields.weekOfWeekBasedYear())}"
            }
            InsightPeriod.MONTHLY -> YearMonth.from(date).toString()
        }
    }

    private data class TimeBucket(
        val key: String,
        val label: String,
        val period: InsightPeriod
    )
}
