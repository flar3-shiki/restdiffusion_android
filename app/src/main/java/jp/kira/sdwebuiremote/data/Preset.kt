package jp.kira.sdwebuiremote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val prompt: String?,
    val negativePrompt: String?,
    val steps: Int?,
    val cfgScale: Float?,
    val width: Int?,
    val height: Int?,
    val sampler: String?,
    val seed: Long?,
    val denoisingStrength: Float?,
    val loras: String? = null,
)
