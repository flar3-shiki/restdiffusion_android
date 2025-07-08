package jp.kira.sdwebuiremote.data

import com.google.gson.annotations.SerializedName

data class SdModel(
    val title: String,
    @SerializedName("model_name")
    val modelName: String,
    val hash: String?,
    val filename: String
)
