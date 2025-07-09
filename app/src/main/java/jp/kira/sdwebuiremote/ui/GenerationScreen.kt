package jp.kira.sdwebuiremote.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jp.kira.sdwebuiremote.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val negativePrompt by viewModel.negativePrompt.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val cfgScale by viewModel.cfgScale.collectAsState()
    val width by viewModel.width.collectAsState()
    val height by viewModel.height.collectAsState()
    val samplers by viewModel.samplers.collectAsState()
    val selectedSampler by viewModel.selectedSampler.collectAsState()
    val seed by viewModel.seed.collectAsState()
    val batchSize by viewModel.batchSize.collectAsState()
    val batchCount by viewModel.batchCount.collectAsState()
    val generationMode by viewModel.generationMode.collectAsState()
    val denoisingStrength by viewModel.denoisingStrength.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val loras by viewModel.loras.collectAsState()
    val selectedLoras by viewModel.selectedLoras.collectAsState()
    val vaes by viewModel.vaes.collectAsState()
    val selectedVae by viewModel.selectedVae.collectAsState()

    var showLoraDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showEmbeddingDialog by remember { mutableStateOf(false) }

    if (showLoraDialog) {
        LoraSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showLoraDialog = false }
        )
    }

    if (showStyleDialog) {
        StyleSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showStyleDialog = false }
        )
    }

    if (showEmbeddingDialog) {
        EmbeddingSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showEmbeddingDialog = false }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.initImageUri.value = uri?.toString()
        }
    )

    LaunchedEffect(uiState) {
        val currentState = uiState
        if (currentState is MainUiState.Error) {
            scope.launch {
                snackbarHostState.showSnackbar(message = currentState.message, duration = SnackbarDuration.Long)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(selectedTabIndex = generationMode.ordinal) {
                Tab(
                    selected = generationMode == GenerationMode.Txt2Img,
                    onClick = { viewModel.generationMode.value = GenerationMode.Txt2Img },
                    text = { Text(stringResource(R.string.txt2img)) }
                )
                Tab(
                    selected = generationMode == GenerationMode.Img2Img,
                    onClick = { viewModel.generationMode.value = GenerationMode.Img2Img },
                    text = { Text(stringResource(R.string.img2img)) }
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                var modelMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = modelMenuExpanded, onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }) {
                    OutlinedTextField(
                        value = selectedModel?.title ?: stringResource(R.string.no_models_found),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.model)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.title) },
                                onClick = {
                                    viewModel.selectedModel.value = model
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (generationMode == GenerationMode.Img2Img) {
                    val initialImageBitmap by viewModel.initialImageBitmap.collectAsState()
                    var brushSize by remember { mutableStateOf(50f) }
                    val paths = remember { mutableStateListOf<Path>() }

                    LaunchedEffect(initialImageBitmap) {
                        paths.clear()
                    }

                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.select_image))
                    }
                    initialImageBitmap?.let { bitmap ->
                        ImageCanvas(
                            baseImage = bitmap,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(256.dp),
                            brushSize = brushSize,
                            paths = paths,
                            onMaskChange = {
                                viewModel.updateMask(it)
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.brush_size, brushSize.toInt()))
                            Button(onClick = { 
                                paths.clear()
                                viewModel.updateMask(null) 
                            }) {
                                Text(stringResource(R.string.clear_mask))
                            }
                        }
                        Slider(
                            value = brushSize,
                            onValueChange = { brushSize = it },
                            valueRange = 10f..200f,
                            steps = 19
                        )
                    }
                    ParameterSlider(label = stringResource(R.string.denoising_strength), value = denoisingStrength, onValueChange = { viewModel.denoisingStrength.value = it }, valueRange = 0f..1f, steps = 100)
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.prompt.value = it },
                    label = { Text(stringResource(R.string.prompt)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { viewModel.negativePrompt.value = it },
                    label = { Text(stringResource(R.string.negative_prompt)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showStyleDialog = true }) {
                        Text(stringResource(R.string.apply_style))
                    }
                    TextButton(onClick = { showEmbeddingDialog = true }) {
                        Text(stringResource(R.string.add_embedding))
                    }
                }

                // --- LoRA Section ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.loras), style = MaterialTheme.typography.titleMedium)
                    selectedLoras.forEach { selectedLora ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedLora.lora.alias)
                                IconButton(onClick = { viewModel.removeLora(selectedLora.lora.name) }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_lora))
                                }
                            }
                            Slider(
                                value = selectedLora.weight,
                                onValueChange = { newWeight ->
                                    viewModel.updateLoraWeight(selectedLora.lora.name, newWeight)
                                },
                                valueRange = -1f..2f,
                                steps = 300
                            )
                        }
                    }
                    Button(
                        onClick = { showLoraDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_lora))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_lora))
                    }
                }

                ParameterSlider(label = stringResource(R.string.steps), value = steps, onValueChange = { viewModel.steps.value = it }, valueRange = 1f..150f, steps = 149)
                ParameterSlider(label = stringResource(R.string.cfg_scale), value = cfgScale, onValueChange = { viewModel.cfgScale.value = it }, valueRange = 1f..30f, steps = 290)

                DimensionControl(
                    label = stringResource(R.string.width),
                    value = width,
                    onValueChange = { viewModel.width.value = it },
                    valueRange = 256f..1024f
                )
                DimensionControl(
                    label = stringResource(R.string.height),
                    value = height,
                    onValueChange = { viewModel.height.value = it },
                    valueRange = 256f..1024f
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberStepper(label = stringResource(R.string.batch_count), value = batchCount, onValueChange = { viewModel.batchCount.value = it }, modifier = Modifier.weight(1f))
                    NumberStepper(label = stringResource(R.string.batch_size), value = batchSize, onValueChange = { viewModel.batchSize.value = it }, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (seed == "-1") "" else seed,
                        onValueChange = { viewModel.seed.value = it },
                        label = { Text(stringResource(R.string.seed)) },
                        placeholder = { Text(stringResource(R.string.seed_random_placeholder)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    IconButton(onClick = { viewModel.seed.value = "-1" }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.random_seed))
                    }
                }

                var samplerMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = samplerMenuExpanded, onExpandedChange = { samplerMenuExpanded = !samplerMenuExpanded }) {
                    OutlinedTextField(
                        value = selectedSampler,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.sampler)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = samplerMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = samplerMenuExpanded, onDismissRequest = { samplerMenuExpanded = false }) {
                        samplers.forEach { samplerName ->
                            DropdownMenuItem(
                                text = { Text(samplerName) },
                                onClick = {
                                    viewModel.selectedSampler.value = samplerName
                                    samplerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.generateImage() },
                    enabled = uiState !is MainUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.generate))
                }

                Button(
                    onClick = { viewModel.addToQueue() },
                    enabled = uiState !is MainUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.add_to_queue))
                }

                when (val state = uiState) {
                    is MainUiState.Idle -> Text(stringResource(R.string.idle_message))
                    is MainUiState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(progress = state.progress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "${(state.progress * 100).toInt()}% (${state.step})")
                        }
                    }
                    is MainUiState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.images) { image ->
                                    Image(
                                        bitmap = image.asImageBitmap(),
                                        contentDescription = stringResource(R.string.generated_image),
                                        modifier = Modifier
                                            .height(300.dp)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                state.images.forEach { viewModel.saveImageToGallery(it) }
                            }) {
                                Text(stringResource(R.string.save_all_to_gallery))
                            }
                        }
                    }
                    is MainUiState.Error -> Text(stringResource(R.string.error_occurred))
                }
            }
        }
    }
}