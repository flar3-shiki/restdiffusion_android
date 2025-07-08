package jp.kira.sdwebuiremote.ui

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jp.kira.sdwebuiremote.data.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    val historyItems by viewModel.displayHistoryItems.collectAsState()
    val displayMode by viewModel.historyDisplayMode.collectAsState()

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
                        viewModel.setHistoryDisplayMode(newMode)
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
        when (displayMode) {
            HistoryDisplayMode.LIST -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyItems) { displayItem ->
                        HistoryListItem(item = displayItem.item) {
                            navController.navigate(Screen.HistoryDetail.createRoute(displayItem.item.id))
                        }
                    }
                }
            }
            HistoryDisplayMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyItems) { displayItem ->
                        HistoryGridItem(displayItem = displayItem, viewModel = viewModel) {
                            navController.navigate(Screen.HistoryDetail.createRoute(displayItem.item.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryGridItem(displayItem: DisplayHistoryItem, viewModel: MainViewModel, onClick: () -> Unit) {
    val item = displayItem.item
    val shouldBlur by viewModel.blurNsfwContent.collectAsState()

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
        }
    }
}

@Composable
fun HistoryListItem(item: HistoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.prompt, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat.getDateTimeInstance().format(Date(item.createdAt)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
