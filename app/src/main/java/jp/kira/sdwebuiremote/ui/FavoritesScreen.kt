package jp.kira.sdwebuiremote.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import jp.kira.sdwebuiremote.ui.history.HistoryViewModel
import jp.kira.sdwebuiremote.ui.history.HistoryViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    mainViewModel: MainViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit,
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val favoriteItems by historyViewModel.favorites.collectAsState()
    val displayMode by mainViewModel.historyDisplayMode.collectAsState() // Reuse the same display mode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val newMode = if (displayMode == HistoryDisplayMode.LIST) HistoryDisplayMode.GRID else HistoryDisplayMode.LIST
                        mainViewModel.setHistoryDisplayMode(newMode)
                    }) {
                        Icon(
                            imageVector = if (displayMode == HistoryDisplayMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (displayMode) {
                HistoryDisplayMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteItems, key = { it.id }) { item ->
                            HistoryListItem(
                                item = item,
                                onDelete = { historyViewModel.deleteHistoryItem(item) },
                                onToggleFavorite = { historyViewModel.toggleFavorite(item) }
                            ) {
                                navController.navigate(Screen.HistoryDetail.createRoute(item.id))
                            }
                        }
                    }
                }
                HistoryDisplayMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteItems, key = { it.id }) { item ->
                            HistoryGridItem(
                                item = item,
                                mainViewModel = mainViewModel, // For NSFW blur
                                onDelete = { historyViewModel.deleteHistoryItem(item) },
                                onToggleFavorite = { historyViewModel.toggleFavorite(item) }
                            ) {
                                navController.navigate(Screen.HistoryDetail.createRoute(item.id))
                            }
                        }
                    }
                }
            }
        }
    }
}
