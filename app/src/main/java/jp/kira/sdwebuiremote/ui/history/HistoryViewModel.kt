package jp.kira.sdwebuiremote.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.kira.sdwebuiremote.data.AppDatabase
import jp.kira.sdwebuiremote.data.HistoryDao
import jp.kira.sdwebuiremote.data.HistoryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao: HistoryDao = AppDatabase.getDatabase(application).historyDao()

    val searchQuery = MutableStateFlow("")
    val modelFilter = MutableStateFlow<String?>(null) // null means no filter

    val modelNames: StateFlow<List<String>> = historyDao.getAllModelNames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historyItems: StateFlow<List<HistoryItem>> =
        combine(
            searchQuery,
            modelFilter
        ) { query, model ->
            historyDao.getHistory(query, model)
        }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistoryItem(historyItem: HistoryItem) {
        viewModelScope.launch {
            historyDao.delete(historyItem)
        }
    }

    val favorites: StateFlow<List<HistoryItem>> = historyDao.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(historyItem: HistoryItem) {
        viewModelScope.launch {
            historyDao.setFavorite(historyItem.id, !historyItem.isFavorite)
        }
    }
}

class HistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}