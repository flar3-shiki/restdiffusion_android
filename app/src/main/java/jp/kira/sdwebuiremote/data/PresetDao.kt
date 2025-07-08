package jp.kira.sdwebuiremote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Insert
    suspend fun insert(preset: Preset)

    @Query("SELECT * FROM presets ORDER BY name ASC")
    fun getAll(): Flow<List<Preset>>

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Int)
}
