package jp.kira.sdwebuiremote.ui

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import jp.kira.sdwebuiremote.data.HistoryItem
import jp.kira.sdwebuiremote.ui.history.HistoryViewModel
import jp.kira.sdwebuiremote.ui.history.HistoryViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    mainViewModel: MainViewModel, // For display mode and NSFW settings
    navController: NavController,
    onNavigateBack: () -> Unit,
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val historyItems by historyViewModel.historyItems.collectAsState()
    val displayMode by mainViewModel.historyDisplayMode.collectAsState()
    val searchQuery by historyViewModel.searchQuery.collectAsState()
    val modelFilter by historyViewModel.modelFilter.collectAsState()
    val modelNames by historyViewModel.modelNames.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
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
            // Search and Filter UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { historyViewModel.searchQuery.value = it },
                    label = { Text("Search Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { historyViewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    }
                )

                var modelMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = modelMenuExpanded, onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }) {
                    OutlinedTextField(
                        value = modelFilter ?: "All Models",
                        onValueChange = {},
                        label = { Text("Model") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("All Models") },
                            onClick = {
                                historyViewModel.modelFilter.value = null
                                modelMenuExpanded = false
                            }
                        )
                        modelNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    historyViewModel.modelFilter.value = name
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            when (displayMode) {
                HistoryDisplayMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyItems, key = { it.id }) { item ->
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
                        items(historyItems, key = { it.id }) { item ->
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

@Composable
fun HistoryGridItem(
    item: HistoryItem,
    mainViewModel: MainViewModel,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val displayHistoryItems by mainViewModel.displayHistoryItems.collectAsState()
    val displayItem = displayHistoryItems.find { it.item.id == item.id } ?: return
    val shouldBlur by mainViewModel.blurNsfwContent.collectAsState()

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageBitmap = BitmapFactory.decodeFile(item.imagePath)?.asImageBitmap()
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = item.prompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (displayItem.isNsfw && shouldBlur) {
                                Modifier.blur(radius = 16.dp)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.prompt,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat.getDateTimeInstance().format(Date(item.createdAt)),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Toggle Favorite",
                        tint = if (item.isFavorite) Color.Yellow else Color.White
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete History Item", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun HistoryListItem(item: HistoryItem, onDelete: () -> Unit, onToggleFavorite: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Model: ${item.modelName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = SimpleDateFormat.getDateTimeInstance().format(Date(item.createdAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Toggle Favorite",
                    tint = if (item.isFavorite) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete History Item")
            }
        }
    }
}