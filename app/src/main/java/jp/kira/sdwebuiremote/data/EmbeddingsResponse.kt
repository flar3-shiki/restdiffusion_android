package jp.kira.sdwebuiremote.data

import com.google.gson.annotations.SerializedName

data class EmbeddingsResponse(
    val loaded: Map<String, EmbeddingDetails>
)

data class EmbeddingDetails(
    val step: Int?,
    @SerializedName("sd_checkpoint")
    val sdCheckpoint: String?,
    @SerializedName("sd_checkpoint_name")
    val sdCheckpointName: String?,
    val shape: Int,
    val vectors: Int
)
