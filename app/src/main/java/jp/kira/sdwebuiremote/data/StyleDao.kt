package jp.kira.sdwebuiremote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleDao {
    @Query("SELECT * FROM styles ORDER BY name ASC")
    fun getAll(): Flow<List<Style>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(style: Style)

    @Update
    suspend fun update(style: Style)

    @Delete
    suspend fun delete(style: Style)
}
