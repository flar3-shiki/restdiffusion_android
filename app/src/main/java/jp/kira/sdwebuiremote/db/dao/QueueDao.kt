package jp.kira.sdwebuiremote.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import jp.kira.sdwebuiremote.db.entity.QueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Insert
    suspend fun insert(queueItem: QueueItem): Long

    @Update
    suspend fun update(queueItem: QueueItem)

    @Delete
    suspend fun delete(queueItem: QueueItem)

    @Query("SELECT * FROM queue ORDER BY queueOrder ASC")
    fun getAll(): Flow<List<QueueItem>>

    @Query("SELECT * FROM queue WHERE status = :status ORDER BY queueOrder ASC LIMIT 1")
    suspend fun getNext(status: String = "waiting"): QueueItem?

    @Update
    suspend fun updateAll(queueItems: List<QueueItem>)

    @Query("SELECT COUNT(*) FROM queue")
    suspend fun getCount(): Int

    @Query("DELETE FROM queue")
    suspend fun clear()

    @Query("DELETE FROM queue WHERE status = 'waiting' AND queueOrder > (SELECT queueOrder FROM queue WHERE status = 'processing' LIMIT 1)")
    suspend fun deleteWaitingAfterProcessing()
}
