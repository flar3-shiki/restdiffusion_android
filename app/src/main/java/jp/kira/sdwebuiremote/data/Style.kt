package jp.kira.sdwebuiremote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "styles")
data class Style(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val prompt: String,
    val negativePrompt: String
)
