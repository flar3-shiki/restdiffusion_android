package jp.kira.sdwebuiremote.data

import com.google.gson.annotations.SerializedName

data class Txt2ImgRequest(
    val prompt: String,
    @SerializedName("negative_prompt")
    val negativePrompt: String = "",
    val steps: Int = 20,
    @SerializedName("cfg_scale")
    val cfgScale: Float = 7.0f,
    @SerializedName("batch_size")
    val batchSize: Int = 1,
    val width: Int = 512,
    val height: Int = 512,
    @SerializedName("sampler_name")
    val samplerName: String = "Euler a",
    val seed: Long = -1,
    @SerializedName("n_iter")
    val batchCount: Int = 1,
    @SerializedName("override_settings")
    val overrideSettings: Map<String, String>? = null
)
