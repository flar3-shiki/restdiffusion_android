package jp.kira.sdwebuiremote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import jp.kira.sdwebuiremote.db.dao.QueueDao
import jp.kira.sdwebuiremote.db.entity.QueueItem

@Database(entities = [HistoryItem::class, Preset::class, Style::class, QueueItem::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun presetDao(): PresetDao
    abstract fun styleDao(): StyleDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sd_webui_remote_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
