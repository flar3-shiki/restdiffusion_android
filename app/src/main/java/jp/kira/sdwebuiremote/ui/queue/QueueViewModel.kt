package jp.kira.sdwebuiremote.ui.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.kira.sdwebuiremote.data.AppDatabase
import jp.kira.sdwebuiremote.data.repository.QueueRepository
import jp.kira.sdwebuiremote.db.entity.QueueItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(application: Application) : AndroidViewModel(application) {

    private val queueDao = AppDatabase.getDatabase(application).queueDao()
    private val queueRepository = QueueRepository(queueDao)

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
