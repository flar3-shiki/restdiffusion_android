package jp.kira.sdwebuiremote.data

import com.google.gson.annotations.SerializedName

data class Img2ImgRequest(
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
    @SerializedName("init_images")
    val initImages: List<String>,
    @SerializedName("denoising_strength")
    val denoisingStrength: Float = 0.75f,
    @SerializedName("override_settings")
    val overrideSettings: Map<String, String>? = null,
    val mask: String? = null,
    @SerializedName("mask_blur")
    val maskBlur: Int = 4,
    @SerializedName("inpainting_fill")
    val inpaintingFill: Int = 1, // 0: fill, 1: original, 2: latent noise, 3: latent nothing
    @SerializedName("inpaint_full_res")
    val inpaintFullRes: Boolean = true,
    @SerializedName("inpainting_mask_invert")
    val inpaintingMaskInvert: Int = 0, // 0: Inpaint masked, 1: Inpaint not masked
)
