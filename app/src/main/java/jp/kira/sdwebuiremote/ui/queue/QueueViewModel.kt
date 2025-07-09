package jp.kira.sdwebuiremote.ui.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.kira.sdwebuiremote.data.AppDatabase
import jp.kira.sdwebuiremote.data.BasicAuthInterceptor
import jp.kira.sdwebuiremote.data.SettingsRepository
import jp.kira.sdwebuiremote.data.StableDiffusionApiService
import jp.kira.sdwebuiremote.data.repository.QueueRepository
import jp.kira.sdwebuiremote.db.entity.QueueItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log

class QueueViewModel(application: Application) : AndroidViewModel(application) {

    private val queueDao = AppDatabase.getDatabase(application).queueDao()
    private val queueRepository = QueueRepository(queueDao)
    private val settingsRepository = SettingsRepository(application)

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

    val queueItems = queueRepository.queue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun delete(queueItem: QueueItem) {
        viewModelScope.launch {
            queueRepository.delete(queueItem)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            queueRepository.clear()
        }
    }

    fun cancelSubsequent() {
        viewModelScope.launch {
            queueRepository.deleteWaitingAfterProcessing()
        }
    }

    fun onMove(from: Int, to: Int) {
        viewModelScope.launch {
            val list = queueItems.value.toMutableList()
            list.add(to, list.removeAt(from))
            val updatedList = list.mapIndexed { index, item ->
                item.copy(queueOrder = index)
            }
            queueRepository.updateAll(updatedList)
        }
    }

    private fun getApiService(baseUrl: String): StableDiffusionApiService {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // Short timeout for interrupt
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)

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

    fun interruptGeneration() {
        viewModelScope.launch {
            try {
                val apiAddress = settingsRepository.apiAddressFlow.first()
                val service = getApiService(apiAddress)
                service.interrupt()
            } catch (e: Exception) {
                // Handle exceptions, maybe show a toast
                Log.e("QueueViewModel", "Failed to interrupt generation", e)
            }
        }
    }
}

class QueueViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QueueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QueueViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
