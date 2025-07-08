package jp.kira.sdwebuiremote.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jp.kira.sdwebuiremote.data.AppDatabase
import jp.kira.sdwebuiremote.data.BasicAuthInterceptor
import jp.kira.sdwebuiremote.data.Img2ImgRequest
import jp.kira.sdwebuiremote.data.SettingsRepository
import jp.kira.sdwebuiremote.data.StableDiffusionApiService
import jp.kira.sdwebuiremote.data.Txt2ImgRequest
import jp.kira.sdwebuiremote.ui.NotificationHelper
import jp.kira.sdwebuiremote.data.repository.QueueRepository
import jp.kira.sdwebuiremote.data.HistoryDao
import jp.kira.sdwebuiremote.data.HistoryItem
import jp.kira.sdwebuiremote.db.entity.QueueItem
import jp.kira.sdwebuiremote.ui.SelectedLora
import jp.kira.sdwebuiremote.util.ImageHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class QueueExecutionService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var queueRepository: QueueRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var historyDao: HistoryDao

    companion object {
        private const val TAG = "QueueExecutionService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service creating...")
        queueRepository = QueueRepository(AppDatabase.getDatabase(this).queueDao())
        settingsRepository = SettingsRepository(this)
        notificationHelper = NotificationHelper(this)
        historyDao = AppDatabase.getDatabase(this).historyDao()
        Log.d(TAG, "onCreate: Service created.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service starting...")
        val notification = notificationHelper.createForegroundNotification("Queue Service", "Starting queue...")
        startForeground(1, notification)

        scope.launch {
            Log.d(TAG, "onStartCommand: Starting queue processing.")
            processQueue()
            Log.d(TAG, "onStartCommand: Queue processing finished. Stopping service.")
            notificationHelper.showCompletionNotification("Queue Finished", "All items have been processed.")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun processQueue() {
        while (true) {
            val nextItem = queueRepository.getNext()
            if (nextItem == null) {
                break
            } else {
                generateImage(nextItem)
            }
        }
    }

    private suspend fun generateImage(item: QueueItem) {
        Log.d(TAG, "generateImage: Processing item ${item.id}")
        try {
            queueRepository.update(item.copy(status = "processing"))
            notificationHelper.showProgressNotification("Queue: Generating...", item.prompt, 0, 100)

            val apiAddress = settingsRepository.apiAddressFlow.first()
            val timeout = settingsRepository.timeoutFlow.first().toLong()
            val username = settingsRepository.usernameFlow.first()
            val password = settingsRepository.passwordFlow.first()

            val service = getApiService(apiAddress, timeout, username, password)

            val loraType = object : TypeToken<List<SelectedLora>>() {}.type
            val selectedLoras: List<SelectedLora> = Gson().fromJson(item.loras, loraType)
            val loraPrompt = selectedLoras.joinToString(" ") { "<lora:${it.lora.name}:${it.weight}>" }
            val finalPrompt = if (loraPrompt.isNotBlank()) "${item.prompt} $loraPrompt" else item.prompt

            val response = if (item.initialImagePath == null) {
                val request = Txt2ImgRequest(
                    prompt = finalPrompt,
                    negativePrompt = item.negativePrompt,
                    steps = item.steps,
                    cfgScale = item.cfgScale,
                    width = item.width,
                    height = item.height,
                    samplerName = item.sampler,
                    seed = item.seed,
                    batchSize = item.batchSize,
                    batchCount = item.batchCount,
                    overrideSettings = mapOf("sd_model_checkpoint" to item.model)
                )
                service.textToImage(request)
            } else {
                val encodedImage = ImageHelper.getBase64FromUri(applicationContext, Uri.parse(item.initialImagePath)) ?: run {
                    throw Exception("Failed to load initial image")
                }
                val request = Img2ImgRequest(
                    prompt = finalPrompt,
                    negativePrompt = item.negativePrompt,
                    steps = item.steps,
                    cfgScale = item.cfgScale,
                    width = item.width,
                    height = item.height,
                    samplerName = item.sampler,
                    seed = item.seed,
                    batchSize = item.batchSize,
                    batchCount = item.batchCount,
                    initImages = listOf(encodedImage),
                    denoisingStrength = item.denoisingStrength ?: 0.75f,
                    overrideSettings = mapOf("sd_model_checkpoint" to item.model)
                )
                service.imageToImage(request)
            }

            if (response.images.isNotEmpty()) {
                val bitmap = response.images.first().let {
                    val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
                val imagePath = ImageHelper.saveImageToInternalStorage(applicationContext, bitmap, "queue_results")
                if (imagePath != null) {
                    val historyItem = HistoryItem(
                        prompt = item.prompt,
                        negativePrompt = item.negativePrompt,
                        steps = item.steps,
                        cfgScale = item.cfgScale,
                        width = item.width,
                        height = item.height,
                        samplerName = item.sampler,
                        seed = item.seed,
                        imagePath = imagePath
                    )
                    historyDao.insert(historyItem)
                }
                queueRepository.update(item.copy(status = "completed", resultImagePath = imagePath))
                notificationHelper.showCompletionNotification("Queue: Item Complete", item.prompt)
            } else {
                queueRepository.update(item.copy(status = "failed"))
                notificationHelper.showCompletionNotification("Queue: Item Failed", "No image received.")
                Log.d(TAG, "generateImage: Item ${item.id} failed. No image received.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "generateImage: Error processing item ${item.id}", e)
            queueRepository.update(item.copy(status = "failed"))
            notificationHelper.showCompletionNotification("Queue: Item Failed", e.message ?: "Unknown error")
        }
    }

    private fun getApiService(baseUrl: String, timeout: Long, user: String, pass: String): StableDiffusionApiService {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)

        if (user.isNotBlank() && pass.isNotBlank()) {
            clientBuilder.addInterceptor(BasicAuthInterceptor(user, pass))
        }

        val okHttpClient = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StableDiffusionApiService::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
