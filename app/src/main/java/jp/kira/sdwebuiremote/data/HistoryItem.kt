package jp.kira.sdwebuiremote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generation_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val negativePrompt: String,
    val steps: Int,
    val cfgScale: Float,
    val width: Int,
    val height: Int,
    val samplerName: String,
    val seed: Long,
    val modelName: String,
    val imagePath: String, // Store the path to the saved image
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
