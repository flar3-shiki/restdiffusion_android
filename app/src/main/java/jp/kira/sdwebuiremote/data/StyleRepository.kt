package jp.kira.sdwebuiremote.data

import kotlinx.coroutines.flow.Flow

class StyleRepository(private val styleDao: StyleDao) {
    val styles: Flow<List<Style>> = styleDao.getAll()

    suspend fun add(style: Style) {
        styleDao.insert(style)
    }

    suspend fun update(style: Style) {
        styleDao.update(style)
    }

    suspend fun delete(style: Style) {
        styleDao.delete(style)
    }
}
