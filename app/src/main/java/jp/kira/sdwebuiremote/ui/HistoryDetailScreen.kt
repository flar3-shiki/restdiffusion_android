package jp.kira.sdwebuiremote.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import jp.kira.sdwebuiremote.data.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    viewModel: MainViewModel,
    navController: NavController,
    historyId: Int,
    onNavigateBack: () -> Unit
) {
    val historyItems by viewModel.displayHistoryItems.collectAsState()
    val historyItem = historyItems.find { it.item.id == historyId }?.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (historyItem != null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val imageBitmap = BitmapFactory.decodeFile(historyItem.imagePath).asImageBitmap()
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Generated Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                Text("Prompt: ${historyItem.prompt}")
                Text("Negative Prompt: ${historyItem.negativePrompt}")
                Text("Steps: ${historyItem.steps}")
                Text("CFG Scale: ${historyItem.cfgScale}")
                Text("Width: ${historyItem.width}")
                Text("Height: ${historyItem.height}")
                Text("Sampler: ${historyItem.samplerName}")
                Text("Seed: ${historyItem.seed}")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveImageToGallery(BitmapFactory.decodeFile(historyItem.imagePath)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save to Gallery")
                }

                Button(
                    onClick = {
                        viewModel.loadSettingsFromHistory(historyItem)
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use These Settings")
                }
            }
        } else {
            // Handle case where history item is not found
            Text("History item not found.")
        }
    }
}
