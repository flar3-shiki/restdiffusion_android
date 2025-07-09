package jp.kira.sdwebuiremote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(historyItem: HistoryItem)

    @Query("SELECT * FROM generation_history ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HistoryItem>>

    @Query("""
        SELECT * FROM generation_history
        WHERE (:query = '' OR prompt LIKE '%' || :query || '%')
        AND (:modelName IS NULL OR modelName = :modelName)
        ORDER BY createdAt DESC
    """)
    fun getHistory(query: String, modelName: String?): Flow<List<HistoryItem>>

    @Delete
    suspend fun delete(historyItem: HistoryItem)

    @Query("SELECT DISTINCT modelName FROM generation_history ORDER BY modelName ASC")
    fun getAllModelNames(): Flow<List<String>>

    @Query("UPDATE generation_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM generation_history WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<HistoryItem>>
}
