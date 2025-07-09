package jp.kira.sdwebuiremote.data.repository

import jp.kira.sdwebuiremote.db.dao.QueueDao
import jp.kira.sdwebuiremote.db.entity.QueueItem
import kotlinx.coroutines.flow.Flow

class QueueRepository(private val queueDao: QueueDao) {
    val queue: Flow<List<QueueItem>> = queueDao.getAll()

    suspend fun add(queueItem: QueueItem) {
        queueDao.insert(queueItem)
    }

    suspend fun update(queueItem: QueueItem) {
        queueDao.update(queueItem)
    }

    suspend fun delete(queueItem: QueueItem) {
        queueDao.delete(queueItem)
    }

    suspend fun getNext(): QueueItem? {
        return queueDao.getNext()
    }

    suspend fun updateAll(queueItems: List<QueueItem>) {
        queueDao.updateAll(queueItems)
    }

    suspend fun getCount(): Int {
        return queueDao.getCount()
    }

    suspend fun clear() {
        queueDao.clear()
    }

    suspend fun deleteWaitingAfterProcessing() {
        queueDao.deleteWaitingAfterProcessing()
    }
}
