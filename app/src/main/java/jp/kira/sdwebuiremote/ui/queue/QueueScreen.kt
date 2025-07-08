package jp.kira.sdwebuiremote.ui.queue

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.kira.sdwebuiremote.db.entity.QueueItem
import jp.kira.sdwebuiremote.service.QueueExecutionService

import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel = viewModel(factory = QueueViewModelFactory(application = LocalContext.current.applicationContext as Application))
) {
    val queueItems by queueViewModel.queueItems.collectAsState()
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        queueViewModel.onMove(from.index, to.index)
    })

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generation Queue") },
                actions = {
                    Button(onClick = { 
                        val intent = Intent(context, QueueExecutionService::class.java)
                        context.startService(intent)
                    }) {
                        Text("Start")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { queueViewModel.clearQueue() }) {
                        Text("Clear All")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .reorderable(state)
                .detectReorderAfterLongPress(state)
        ) {
            items(queueItems, { it.id }) { item ->
                ReorderableItem(state, key = item.id) {
                    QueueListItem(item = item, onDelete = { queueViewModel.delete(item) })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun QueueListItem(item: QueueItem, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = item.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = "Status: ${item.status} | Sampler: ${item.sampler} | Seed: ${item.seed}")
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Queued Item")
            }
        }
    )
}
