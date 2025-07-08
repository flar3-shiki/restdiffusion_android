package jp.kira.sdwebuiremote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(historyItem: HistoryItem)

    @Query("SELECT * FROM generation_history ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HistoryItem>>
}
