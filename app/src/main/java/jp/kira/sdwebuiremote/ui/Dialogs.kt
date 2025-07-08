package jp.kira.sdwebuiremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import jp.kira.sdwebuiremote.R

@Composable
fun SavePresetDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val parameters = remember {
        mutableStateMapOf(
            "prompt" to true,
            "negativePrompt" to true,
            "steps" to true,
            "cfgScale" to true,
            "width" to true,
            "height" to true,
            "sampler" to true,
            "seed" to true,
            "denoisingStrength" to true,
            "loras" to true
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_preset)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.preset_name)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.parameters_to_include))
                LazyColumn {
                    items(parameters.keys.toList()) { key ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    parameters[key] = !(parameters[key] ?: false)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = parameters[key] ?: false,
                                onCheckedChange = { checked -> parameters[key] = checked }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(key)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lorasString = if (parameters["loras"] == true) {
                        viewModel.selectedLoras.value.joinToString(",") {
                            "${it.lora.name}:${it.weight}"
                        }
                    } else {
                        null
                    }
                    viewModel.savePreset(
                        name = name,
                        prompt = if (parameters["prompt"] == true) viewModel.prompt.value else null,
                        negativePrompt = if (parameters["negativePrompt"] == true) viewModel.negativePrompt.value else null,
                        steps = if (parameters["steps"] == true) viewModel.steps.value.toInt() else null,
                        cfgScale = if (parameters["cfgScale"] == true) viewModel.cfgScale.value else null,
                        width = if (parameters["width"] == true) viewModel.width.value.toIntOrNull() else null,
                        height = if (parameters["height"] == true) viewModel.height.value.toIntOrNull() else null,
                        sampler = if (parameters["sampler"] == true) viewModel.selectedSampler.value else null,
                        seed = if (parameters["seed"] == true) viewModel.seed.value.toLongOrNull() else null,
                        denoisingStrength = if (parameters["denoisingStrength"] == true) viewModel.denoisingStrength.value else null,
                        loras = lorasString
                    )
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LoadPresetDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val presets by viewModel.presets.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text(stringResource(R.string.load_preset_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(presets) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.applyPreset(preset)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(preset.name)
                            IconButton(onClick = { viewModel.deletePreset(preset) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_preset))
                            }
                        }
                    }
                }
                Row(modifier = Modifier.align(Alignment.End).padding(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun LoraSelectionDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val loras by viewModel.loras.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text(stringResource(R.string.select_lora), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(loras) { lora ->
                        Text(
                            text = lora.alias,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addLora(lora)
                                    onDismiss()
                                }
                                .padding(16.dp)
                        )
                    }
                }
                Row(modifier = Modifier.align(Alignment.End).padding(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun StyleSelectionDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val styles by viewModel.styles.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text(stringResource(R.string.apply_style), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(styles) { style ->
                        Text(
                            text = style.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.applyStyle(style)
                                    onDismiss()
                                }
                                .padding(16.dp)
                        )
                    }
                }
                Row(modifier = Modifier.align(Alignment.End).padding(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}