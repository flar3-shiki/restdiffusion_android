package jp.kira.sdwebuiremote.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val negativePrompt: String,
    val steps: Int,
    val sampler: String,
    val cfgScale: Float,
    val seed: Long,
    val width: Int,
    val height: Int,
    val model: String,
    val loras: String, // JSON a string for simplicity
    val batchSize: Int,
    val batchCount: Int,
    val denoisingStrength: Float?, // Nullable for Txt2Img
    val initialImagePath: String?, // Nullable for Txt2Img
    val status: String, // e.g., "waiting", "processing", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val resultImagePath: String? = null,
    val queueOrder: Int = 0
)
