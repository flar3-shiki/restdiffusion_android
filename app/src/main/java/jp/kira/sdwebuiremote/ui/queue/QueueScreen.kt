package jp.kira.sdwebuiremote.ui.queue

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.kira.sdwebuiremote.R
import jp.kira.sdwebuiremote.db.entity.QueueItem
import jp.kira.sdwebuiremote.service.QueueExecutionService
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel = viewModel(factory = QueueViewModelFactory(application = LocalContext.current.applicationContext as Application))
) {
    val queueItems by queueViewModel.queueItems.collectAsState()
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        queueViewModel.onMove(from.index, to.index)
    })

    val context = LocalContext.current
    val isProcessing = queueItems.any { it.status == "processing" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue_title)) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { 
                    val intent = Intent(context, QueueExecutionService::class.java)
                    context.startService(intent)
                }) {
                    Text(stringResource(R.string.queue_start))
                }
                Button(
                    onClick = { queueViewModel.interruptGeneration() },
                    enabled = isProcessing
                ) {
                    Text(stringResource(R.string.interrupt))
                }
                Button(
                    onClick = { queueViewModel.cancelSubsequent() },
                    enabled = isProcessing
                ) {
                    Text(stringResource(R.string.cancel_subsequent))
                }
                Button(onClick = { queueViewModel.clearQueue() }) {
                    Text(stringResource(R.string.queue_clear_all))
                }
            }
            Divider()
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
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
}

@Composable
fun QueueListItem(item: QueueItem, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = item.prompt, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = stringResource(id = R.string.queue_item_details, item.status, item.sampler, item.seed))
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.queue_delete_item_cd))
            }
        }
    )
}