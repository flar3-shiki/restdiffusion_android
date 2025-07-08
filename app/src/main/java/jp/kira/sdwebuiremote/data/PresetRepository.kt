package jp.kira.sdwebuiremote.data

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {

    fun getAllPresets(): Flow<List<Preset>> {
        return presetDao.getAll()
    }

    suspend fun insert(preset: Preset) {
        presetDao.insert(preset)
    }

    suspend fun delete(id: Int) {
        presetDao.delete(id)
    }
}
