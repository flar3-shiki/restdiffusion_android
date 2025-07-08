package jp.kira.sdwebuiremote.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.InputStream

class PngInfoViewModel : ViewModel() {
    var pngInfo by mutableStateOf<String?>(null)

    fun loadPngInfo(uri: Uri, context: Context) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // Simple text chunk search for "parameters"
                val bytes = stream.readBytes()
                val text = String(bytes, Charsets.ISO_8859_1)
                val key = "parameters"
                val index = text.indexOf(key)
                if (index != -1) {
                    val startIndex = index + key.length
                    // Find the null terminator for the text chunk
                    val endIndex = text.indexOf(0.toChar(), startIndex)
                    if (endIndex != -1) {
                        pngInfo = text.substring(startIndex, endIndex).trim()
                    } else {
                        pngInfo = "Could not find end of parameters chunk."
                    }
                } else {
                    pngInfo = "No Stable Diffusion parameters found in this image."
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pngInfo = "Error reading PNG info: ${e.message}"
        }
    }
}

@Composable
fun PngInfoScreen(viewModel: PngInfoViewModel = viewModel()) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.loadPngInfo(it, context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { launcher.launch("image/png") }) {
            Text("Select PNG Image")
        }

        viewModel.pngInfo?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}