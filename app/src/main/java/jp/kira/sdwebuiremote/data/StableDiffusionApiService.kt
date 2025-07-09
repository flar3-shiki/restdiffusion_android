package jp.kira.sdwebuiremote.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

import retrofit2.http.Query

interface StableDiffusionApiService {
    @POST("/sdapi/v1/txt2img")
    suspend fun textToImage(@Body request: Txt2ImgRequest): Txt2ImgResponse

    @GET("/sdapi/v1/samplers")
    suspend fun getSamplers(): List<Sampler>

    @GET("/sdapi/v1/progress")
    suspend fun getProgress(@Query("skip_current_image") skipCurrentImage: Boolean = true): ProgressResponse

    @GET("/sdapi/v1/sd-models")
    suspend fun getSdModels(): List<SdModel>

    @GET("/sdapi/v1/loras")
    suspend fun getLoras(): List<Lora>

    @POST("/sdapi/v1/img2img")
    suspend fun imageToImage(@Body request: Img2ImgRequest): Txt2ImgResponse

    @GET("sdapi/v1/sd-vae")
    suspend fun getVaes(): List<Vae>

    @GET("sdapi/v1/embeddings")
    suspend fun getEmbeddings(): EmbeddingsResponse

    @POST("sdapi/v1/interrupt")
    suspend fun interrupt()
}
