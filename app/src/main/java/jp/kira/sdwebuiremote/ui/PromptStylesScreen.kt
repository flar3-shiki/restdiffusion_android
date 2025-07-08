package jp.kira.sdwebuiremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kira.sdwebuiremote.data.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptStylesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val styles by viewModel.styles.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingStyle by remember { mutableStateOf<Style?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt Styles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingStyle = null
                showDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Style")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(styles) { style ->
                StyleListItem(
                    style = style,
                    onEdit = {
                        editingStyle = style
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteStyle(style) }
                )
            }
        }

        if (showDialog) {
            StyleEditDialog(
                style = editingStyle,
                onDismiss = { showDialog = false },
                onSave = {
                    if (editingStyle == null) {
                        viewModel.addStyle(it.name, it.prompt, it.negativePrompt)
                    } else {
                        viewModel.updateStyle(it)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun StyleListItem(
    style: Style,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = style.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Style")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Style")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (style.prompt.isNotBlank()) {
                Text(text = "Prompt: ${style.prompt}", style = MaterialTheme.typography.bodyMedium)
            }
            if (style.negativePrompt.isNotBlank()) {
                Text(text = "Negative: ${style.negativePrompt}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StyleEditDialog(
    style: Style?,
    onDismiss: () -> Unit,
    onSave: (Style) -> Unit
) {
    var name by remember { mutableStateOf(style?.name ?: "") }
    var prompt by remember { mutableStateOf(style?.prompt ?: "") }
    var negativePrompt by remember { mutableStateOf(style?.negativePrompt ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (style == null) "Add Style" else "Edit Style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Style Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.height(100.dp)
                )
                OutlinedTextField(
                    value = negativePrompt,
                    onValueChange = { negativePrompt = it },
                    label = { Text("Negative Prompt") },
                    modifier = Modifier.height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStyle = style?.copy(
                        name = name,
                        prompt = prompt,
                        negativePrompt = negativePrompt
                    ) ?: Style(name = name, prompt = prompt, negativePrompt = negativePrompt)
                    onSave(newStyle)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}