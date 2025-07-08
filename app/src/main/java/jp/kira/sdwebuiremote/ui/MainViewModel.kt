package jp.kira.sdwebuiremote.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.kira.sdwebuiremote.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import jp.kira.sdwebuiremote.data.repository.QueueRepository
import jp.kira.sdwebuiremote.db.entity.QueueItem
import jp.kira.sdwebuiremote.util.ImageHelper
import java.util.concurrent.TimeUnit

// 画像生成の状態
sealed interface MainUiState {
    object Idle : MainUiState
    data class Loading(val progress: Float, val step: String) : MainUiState
    data class Success(val images: List<Bitmap>) : MainUiState
    data class Error(val message: String) : MainUiState
}

// APIサーバーとの接続状態
sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

// 生成モード
enum class GenerationMode {
    Txt2Img,
    Img2Img
}

data class DisplayHistoryItem(val item: HistoryItem, val isNsfw: Boolean)

enum class HistoryDisplayMode { LIST, GRID }

data class SelectedLora(val lora: Lora, val weight: Float)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val historyDao = AppDatabase.getDatabase(application).historyDao()
    private val presetDao = AppDatabase.getDatabase(application).presetDao()
    private val presetRepository = PresetRepository(presetDao)
    private val styleDao = AppDatabase.getDatabase(application).styleDao()
    private val styleRepository = StyleRepository(styleDao)
    private val queueDao = AppDatabase.getDatabase(application).queueDao()
    private val queueRepository = QueueRepository(queueDao)
    private val notificationHelper = NotificationHelper(application)
    private var progressPollingJob: Job? = null
    private var connectionJob: Job? = null

    // --- State Flows ---
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // --- Generation Mode ---
    val generationMode = MutableStateFlow(GenerationMode.Txt2Img)

    // --- Img2Img Parameters ---
    val initImageUri = MutableStateFlow<String?>(null)
    val denoisingStrength = MutableStateFlow(0.75f)

    private val _initialImageBitmap = MutableStateFlow<Bitmap?>(null)
    val initialImageBitmap = _initialImageBitmap.asStateFlow()

    private val _maskBitmap = MutableStateFlow<Bitmap?>(null)
    val maskBitmap = _maskBitmap.asStateFlow()

    fun updateMask(bitmap: Bitmap?) {
        _maskBitmap.value = bitmap
    }

    // --- Input Parameters ---
    val prompt = MutableStateFlow("a cute cat, best quality, masterpiece")
    val negativePrompt = MutableStateFlow("worst quality, low quality, normal quality, ugly, blurry")
    val steps = MutableStateFlow(25f)
    val cfgScale = MutableStateFlow(7.0f)
    val width = MutableStateFlow("512")
    val height = MutableStateFlow("512")
    val selectedSampler = MutableStateFlow("Euler a")
    val seed = MutableStateFlow("-1")
    val batchSize = MutableStateFlow("1")
    val batchCount = MutableStateFlow("1")

    private val _samplers = MutableStateFlow<List<String>>(emptyList())
    val samplers = _samplers.asStateFlow()

    private val _models = MutableStateFlow<List<SdModel>>(emptyList())
    val models = _models.asStateFlow()

    private val _loras = MutableStateFlow<List<Lora>>(emptyList())
    val loras = _loras.asStateFlow()

    private val _selectedLoras = MutableStateFlow<List<SelectedLora>>(emptyList())
    val selectedLoras = _selectedLoras.asStateFlow()

    val selectedModel = MutableStateFlow<SdModel?>(null)

    // --- History ---
    val historyItems = historyDao.getAll()

    val nsfwKeywords = settingsRepository.nsfwKeywordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val displayHistoryItems = historyItems.combine(nsfwKeywords) { list, keywords ->
        list.map {
            val isNsfw = keywords.any { keyword -> it.prompt.contains(keyword, ignoreCase = true) }
            DisplayHistoryItem(it, isNsfw)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _historyDisplayMode = MutableStateFlow(HistoryDisplayMode.LIST)
    val historyDisplayMode = _historyDisplayMode.asStateFlow()

    fun setHistoryDisplayMode(mode: HistoryDisplayMode) {
        _historyDisplayMode.value = mode
    }

    // --- Presets ---
    val presets = presetRepository.getAllPresets().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val showSavePresetDialog = MutableStateFlow(false)
    val showLoadPresetDialog = MutableStateFlow(false)

    // --- Styles ---
    val styles = styleRepository.styles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun savePreset(
        name: String,
        prompt: String?,
        negativePrompt: String?,
        steps: Int?,
        cfgScale: Float?,
        width: Int?,
        height: Int?,
        sampler: String?,
        seed: Long?,
        denoisingStrength: Float?,
        loras: String?
    ) {
        viewModelScope.launch {
            val preset = Preset(
                name = name,
                prompt = prompt,
                negativePrompt = negativePrompt,
                steps = steps,
                cfgScale = cfgScale,
                width = width,
                height = height,
                sampler = sampler,
                seed = seed,
                denoisingStrength = denoisingStrength,
                loras = loras
            )
            presetRepository.insert(preset)
        }
    }

    fun applyPreset(preset: Preset) {
        preset.prompt?.let { prompt.value = it }
        preset.negativePrompt?.let { negativePrompt.value = it }
        preset.steps?.let { steps.value = it.toFloat() }
        preset.cfgScale?.let { cfgScale.value = it }
        preset.width?.let { width.value = it.toString() }
        preset.height?.let { height.value = it.toString() }
        preset.sampler?.let { selectedSampler.value = it }
        preset.seed?.let { seed.value = it.toString() }
        preset.denoisingStrength?.let { denoisingStrength.value = it }

        val restoredLoras = mutableListOf<SelectedLora>()
        preset.loras?.split(',')?.forEach { loraString ->
            val parts = loraString.split(':')
            if (parts.size == 2) {
                val loraName = parts[0]
                val loraWeight = parts[1].toFloatOrNull()
                val lora = loras.value.find { it.name == loraName }
                if (lora != null && loraWeight != null) {
                    restoredLoras.add(SelectedLora(lora, loraWeight))
                }
            }
        }
        _selectedLoras.value = restoredLoras
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            presetRepository.delete(preset.id)
        }
    }

    fun addLora(lora: Lora) {
        if (_selectedLoras.value.none { it.lora.name == lora.name }) {
            _selectedLoras.value = _selectedLoras.value + SelectedLora(lora, 0.8f)
        }
    }

    fun removeLora(loraName: String) {
        _selectedLoras.value = _selectedLoras.value.filterNot { it.lora.name == loraName }
    }

    fun updateLoraWeight(loraName: String, weight: Float) {
        val updatedList = _selectedLoras.value.map {
            if (it.lora.name == loraName) {
                it.copy(weight = weight)
            } else {
                it
            }
        }
        _selectedLoras.value = updatedList
    }

    // --- Settings ---
    val apiAddress = settingsRepository.apiAddressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "http://10.0.2.2:7860"
    )
    val timeoutSeconds = settingsRepository.timeoutFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 60
    )
    val themeSetting = settingsRepository.themeSettingFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeSetting.System
    )

    val blurNsfwContent = settingsRepository.blurNsfwContentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val username = settingsRepository.usernameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val password = settingsRepository.passwordFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    init {
        notificationHelper.createNotificationChannel()

        viewModelScope.launch {
            initImageUri.collect { uriString ->
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    _initialImageBitmap.value = ImageHelper.getBitmapFromUri(getApplication(), uri)
                } else {
                    _initialImageBitmap.value = null
                }
                // When the initial image changes, clear the mask.
                _maskBitmap.value = null
            }
        }
    }

    private fun getApiService(baseUrl: String, timeout: Long): StableDiffusionApiService {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)

        val user = username.value
        val pass = password.value
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

    fun connect() {
        connectionJob = viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            try {
                val currentApiAddress = settingsRepository.apiAddressFlow.first()
                val currentTimeout = settingsRepository.timeoutFlow.first()
                val service = getApiService(currentApiAddress, currentTimeout.toLong())
                // Get samplers and models in parallel
                val samplersJob = viewModelScope.launch { 
                    val samplerList = service.getSamplers()
                    _samplers.value = samplerList.map { it.name }
                    if (samplerList.isNotEmpty() && !samplerList.any { it.name == selectedSampler.value }) {
                        selectedSampler.value = samplerList.first().name
                    }
                }
                val modelsJob = viewModelScope.launch {
                    val modelList = service.getSdModels()
                    _models.value = modelList
                    if (modelList.isNotEmpty()) {
                        selectedModel.value = modelList.first()
                    }
                }
                val lorasJob = viewModelScope.launch {
                    _loras.value = service.getLoras()
                }
                samplersJob.join()
                modelsJob.join()
                lorasJob.join()

                _connectionState.value = ConnectionState.Connected
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Don't update state if the job was cancelled
                    return@launch
                }
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            }
        }
    }

    fun cancelConnection() {
        connectionJob?.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = viewModelScope.launch {
            val currentApiAddress = settingsRepository.apiAddressFlow.first()
            val currentTimeout = settingsRepository.timeoutFlow.first()
            val service = getApiService(currentApiAddress, currentTimeout.toLong())
            while (true) {
                try {
                    val progress = service.getProgress()
                    val step = "${progress.state.samplingStep}/${progress.state.samplingSteps}"
                    _uiState.value = MainUiState.Loading(progress.progress, step)
                    notificationHelper.showProgressNotification(
                        "Generation in progress...",
                        "${(progress.progress * 100).toInt()}% ($step)",
                        (progress.progress * 100).toInt(),
                        100
                    )
                    if (progress.progress >= 1.0f) break
                } catch (e: Exception) {
                    // Ignore polling errors
                }
                delay(1000)
            }
        }
    }

    fun generateImage() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading(0f, "0/${steps.value.toInt()}")
            notificationHelper.showProgressNotification("Generation Started", "Your image is being generated.", 0, 100)
            startProgressPolling()
            try {
                val currentApiAddress = settingsRepository.apiAddressFlow.first()
                val service = getApiService(currentApiAddress, 0L) // 0L for infinite timeout
                val overrideSettings = selectedModel.value?.let {
                    mapOf("sd_model_checkpoint" to it.modelName)
                } ?: emptyMap()

                val loraPrompt = selectedLoras.value.joinToString(" ") { "<lora:${it.lora.name}:${it.weight}>" }
                val finalPrompt = if (loraPrompt.isNotBlank()) "${prompt.value} $loraPrompt" else prompt.value

                val response = when (generationMode.value) {
                    GenerationMode.Txt2Img -> {
                        val request = Txt2ImgRequest(
                            prompt = finalPrompt,
                            negativePrompt = negativePrompt.value,
                            steps = steps.value.toInt(),
                            cfgScale = cfgScale.value,
                            width = width.value.toIntOrNull() ?: 512,
                            height = height.value.toIntOrNull() ?: 512,
                            samplerName = selectedSampler.value,
                            seed = seed.value.toLongOrNull() ?: -1,
                            batchSize = batchSize.value.toIntOrNull() ?: 1,
                            batchCount = batchCount.value.toIntOrNull() ?: 1,
                            overrideSettings = overrideSettings
                        )
                        service.textToImage(request)
                    }
                    GenerationMode.Img2Img -> {
                        val imageUri = initImageUri.value ?: run {
                            _uiState.value = MainUiState.Error("Initial image not selected.")
                            return@launch
                        }
                        val encodedImage = ImageHelper.getBase64FromUri(getApplication(), Uri.parse(imageUri)) ?: run {
                            _uiState.value = MainUiState.Error("Failed to load image.")
                            return@launch
                        }

                        val request = Img2ImgRequest(
                            prompt = finalPrompt,
                            negativePrompt = negativePrompt.value,
                            steps = steps.value.toInt(),
                            cfgScale = cfgScale.value,
                            width = width.value.toIntOrNull() ?: 512,
                            height = height.value.toIntOrNull() ?: 512,
                            samplerName = selectedSampler.value,
                            seed = seed.value.toLongOrNull() ?: -1,
                            batchSize = batchSize.value.toIntOrNull() ?: 1,
                            batchCount = batchCount.value.toIntOrNull() ?: 1,
                            initImages = listOf(encodedImage),
                            denoisingStrength = denoisingStrength.value,
                            overrideSettings = overrideSettings,
                            mask = maskBitmap.value?.let { ImageHelper.bitmapToBase64(it) }
                        )
                        service.imageToImage(request)
                    }
                }

                progressPollingJob?.cancel()

                if (response.images.isNotEmpty()) {
                    val bitmaps = response.images.map {
                        val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }

                    bitmaps.forEach { bitmap ->
                        val imagePath = ImageHelper.saveImageToInternalStorage(getApplication(), bitmap, "history")
                        if (imagePath != null) {
                            val historyItem = HistoryItem(
                                prompt = prompt.value,
                                negativePrompt = negativePrompt.value,
                                steps = steps.value.toInt(),
                                cfgScale = cfgScale.value,
                                width = width.value.toIntOrNull() ?: 512,
                                height = height.value.toIntOrNull() ?: 512,
                                samplerName = selectedSampler.value,
                                seed = seed.value.toLongOrNull() ?: -1,
                                imagePath = imagePath
                            )
                            historyDao.insert(historyItem)
                        }
                    }

                    _uiState.value = MainUiState.Success(bitmaps)
                    notificationHelper.showCompletionNotification("Generation Complete", "${bitmaps.size} images are ready!")
                } else {
                    _uiState.value = MainUiState.Error("No images received from API.")
                    notificationHelper.showCompletionNotification("Generation Failed", "No images were returned from the API.")
                }

            } catch (e: Exception) {
                progressPollingJob?.cancel()
                val errorMessage = e.message ?: "An unknown error occurred."
                _uiState.value = MainUiState.Error(errorMessage)
                notificationHelper.showCompletionNotification("Generation Failed", errorMessage)
            }
        }
    }

    /*
    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        val context = getApplication<Application>().applicationContext
        val directory = context.getDir("history", Context.MODE_PRIVATE)
        val fileName = "history_${System.currentTimeMillis()}.png"
        val file = File(directory, fileName)
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    */

    fun saveImageToGallery(bitmap: Bitmap) {
        val context = getApplication<Application>().applicationContext
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "generated_image_${System.currentTimeMillis()}.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/RestDiffusion")
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadSettingsFromHistory(historyItem: HistoryItem) {
        prompt.value = historyItem.prompt
        negativePrompt.value = historyItem.negativePrompt
        steps.value = historyItem.steps.toFloat()
        cfgScale.value = historyItem.cfgScale
        width.value = historyItem.width.toString()
        height.value = historyItem.height.toString()
        selectedSampler.value = historyItem.samplerName
        seed.value = historyItem.seed.toString()
    }

    fun saveTimeout(timeout: Int) {
        viewModelScope.launch {
            settingsRepository.saveTimeout(timeout)
        }
    }

    fun saveApiAddress(address: String) {
        viewModelScope.launch {
            settingsRepository.saveApiAddress(address)
        }
    }

    fun saveThemeSetting(theme: ThemeSetting) {
        viewModelScope.launch {
            settingsRepository.saveThemeSetting(theme)
        }
    }

    fun saveBlurNsfwContent(shouldBlur: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveBlurNsfwContent(shouldBlur)
        }
    }

    fun addNsfwKeyword(keyword: String) {
        viewModelScope.launch {
            if (keyword.isNotBlank()) {
                val currentKeywords = nsfwKeywords.value
                settingsRepository.saveNsfwKeywords(currentKeywords + keyword.lowercase())
            }
        }
    }

    fun removeNsfwKeyword(keyword: String) {
        viewModelScope.launch {
            val currentKeywords = nsfwKeywords.value
            settingsRepository.saveNsfwKeywords(currentKeywords - keyword)
        }
    }

    fun resetNsfwKeywords() {
        viewModelScope.launch {
            settingsRepository.saveNsfwKeywords(SettingsRepository.DEFAULT_NSFW_KEYWORDS)
        }
    }

    fun saveCredentials(username: String, password: String) {
        viewModelScope.launch {
            settingsRepository.saveCredentials(username, password)
        }
    }

    fun handleSharedImage(uri: Uri?) {
        if (uri == null) return
        // This will trigger the collector to update the bitmap
        initImageUri.value = uri.toString()
        generationMode.value = GenerationMode.Img2Img
    }

    fun addStyle(name: String, prompt: String, negativePrompt: String) {
        viewModelScope.launch {
            val style = Style(name = name, prompt = prompt, negativePrompt = negativePrompt)
            styleRepository.add(style)
        }
    }

    fun updateStyle(style: Style) {
        viewModelScope.launch {
            styleRepository.update(style)
        }
    }

    fun deleteStyle(style: Style) {
        viewModelScope.launch {
            styleRepository.delete(style)
        }
    }

    fun applyStyle(style: Style) {
        val currentPrompt = prompt.value
        val currentNegativePrompt = negativePrompt.value

        // Append style prompts, adding a comma if the current prompt is not empty.
        if (style.prompt.isNotBlank()) {
            prompt.value = if (currentPrompt.isBlank()) {
                style.prompt
            } else {
                "$currentPrompt, ${style.prompt}"
            }
        }

        if (style.negativePrompt.isNotBlank()) {
            negativePrompt.value = if (currentNegativePrompt.isBlank()) {
                style.negativePrompt
            } else {
                "$currentNegativePrompt, ${style.negativePrompt}"
            }
        }
    }

    fun addToQueue() {
        viewModelScope.launch {
            val count = queueRepository.getCount()
            val loraJson = Gson().toJson(selectedLoras.value)
            val item = QueueItem(
                prompt = prompt.value,
                negativePrompt = negativePrompt.value,
                steps = steps.value.toInt(),
                sampler = selectedSampler.value,
                cfgScale = cfgScale.value,
                seed = seed.value.toLongOrNull() ?: -1,
                width = width.value.toIntOrNull() ?: 512,
                height = height.value.toIntOrNull() ?: 512,
                model = selectedModel.value?.modelName ?: "",
                loras = loraJson,
                batchSize = batchSize.value.toIntOrNull() ?: 1,
                batchCount = batchCount.value.toIntOrNull() ?: 1,
                denoisingStrength = if (generationMode.value == GenerationMode.Img2Img) denoisingStrength.value else null,
                initialImagePath = if (generationMode.value == GenerationMode.Img2Img) initImageUri.value else null,
                status = "waiting",
                queueOrder = count
            )
            queueRepository.add(item)
            val context = getApplication<Application>().applicationContext
            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
