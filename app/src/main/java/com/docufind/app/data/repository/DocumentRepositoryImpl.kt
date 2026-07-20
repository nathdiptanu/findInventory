package com.docufind.app.data.repository

import com.docufind.app.data.local.db.dao.EmergencyContactDao
import com.docufind.app.data.local.db.dao.FamilyMemberDao
import com.docufind.app.data.local.db.dao.PetDao
import com.docufind.app.data.local.db.dao.ReminderDao
import com.docufind.app.data.local.db.dao.VaultRecordDao
import com.docufind.app.domain.model.CategorySummary
import com.docufind.app.domain.model.DocumentCategory
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.QuickAccessSummary
import com.docufind.app.domain.model.RecentDocument
import com.docufind.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val vaultRecordDao: VaultRecordDao,
    private val reminderDao: ReminderDao,
    private val familyMemberDao: FamilyMemberDao,
    private val petDao: PetDao,
    private val emergencyContactDao: EmergencyContactDao
) : DocumentRepository {

    override fun observeCategorySummaries(): Flow<List<CategorySummary>> {
        val flows = DocumentCategory.entries.map { category ->
            vaultRecordDao.observeCountByCategory(category.id).map { count ->
                CategorySummary(category, count)
            }
        }
        return combine(flows) { summaries -> summaries.toList() }
    }

    override fun observeQuickAccessSummaries(): Flow<List<QuickAccessSummary>> {
        val documentFlows = QuickAccessItem.homeGridOrder
            .filter { it != QuickAccessItem.MORE }
            .map { item ->
                when (item) {
                    QuickAccessItem.PETS -> petDao.observeCount().map { QuickAccessSummary(item, it) }
                    QuickAccessItem.FAMILY -> familyMemberDao.observeCount().map { QuickAccessSummary(item, it) }
                    QuickAccessItem.EMERGENCY -> emergencyContactDao.observeCount().map { QuickAccessSummary(item, it) }
                    QuickAccessItem.REMINDERS -> reminderDao.observeActiveCount().map { QuickAccessSummary(item, it) }
                    else -> vaultRecordDao.observeCountByCategory(item.id).map { QuickAccessSummary(item, it) }
                }
            }
        val moreFlow = vaultRecordDao.observeTotalCount().map { total ->
            QuickAccessSummary(QuickAccessItem.MORE, total)
        }
        return combine(documentFlows + moreFlow) { summaries ->
            QuickAccessItem.homeGridOrder.map { item ->
                summaries.find { it.item == item } ?: QuickAccessSummary(item, 0)
            }
        }
    }

    override fun observeRecentDocuments(limit: Int): Flow<List<RecentDocument>> =
        vaultRecordDao.observeRecent(limit).map { records ->
            records.map { record ->
                RecentDocument(
                    id = record.id,
                    title = record.title,
                    categoryId = record.category,
                    updatedAt = record.updatedAt
                )
            }
        }
}
