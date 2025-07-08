package jp.kira.sdwebuiremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import jp.kira.sdwebuiremote.R
import jp.kira.sdwebuiremote.data.ThemeSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPromptStyles: () -> Unit
) {
    // States from ViewModel
    val timeout by viewModel.timeoutSeconds.collectAsState()
    val apiAddress by viewModel.apiAddress.collectAsState()
    val currentTheme by viewModel.themeSetting.collectAsState()
    val blurNsfw by viewModel.blurNsfwContent.collectAsState()
    val nsfwKeywords by viewModel.nsfwKeywords.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    // Local states for UI interaction
    var sliderPosition by remember(timeout) { mutableStateOf(timeout.toFloat()) }
    var apiAddressText by remember(apiAddress) { mutableStateOf(apiAddress) }
    var newKeyword by remember { mutableStateOf("") }
    var usernameText by remember(username) { mutableStateOf(username) }
    var passwordText by remember(password) { mutableStateOf(password) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Network Timeout Section -- -
            Text(stringResource(R.string.network_timeout), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 10f..300f,
                    steps = 289,
                    modifier = Modifier.weight(1f),
                    onValueChangeFinished = {
                        viewModel.saveTimeout(sliderPosition.toInt())
                        focusManager.clearFocus()
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("${sliderPosition.toInt()}s", modifier = Modifier.width(40.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- API Endpoint Section ---
            Text(stringResource(R.string.api_endpoint), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = apiAddressText,
                onValueChange = { apiAddressText = it },
                label = { Text(stringResource(R.string.api_endpoint_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && apiAddressText != apiAddress) {
                            viewModel.saveApiAddress(apiAddressText)
                        }
                    },
                singleLine = true,
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Authentication Section ---
            Text(stringResource(R.string.authentication), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = usernameText,
                onValueChange = { usernameText = it },
                label = { Text(stringResource(R.string.username_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && (usernameText != username || passwordText != password)) {
                            viewModel.saveCredentials(usernameText, passwordText)
                        }
                    },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
                label = { Text(stringResource(R.string.password_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && (usernameText != username || passwordText != password)) {
                            viewModel.saveCredentials(usernameText, passwordText)
                        }
                    },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Theme Section ---
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
            Column {
                ThemeSetting.values().forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.saveThemeSetting(theme) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (currentTheme == theme),
                            onClick = { viewModel.saveThemeSetting(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = theme.name)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Prompt Styles Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToPromptStyles)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.prompt_styles),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.manage_prompt_styles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.manage_prompt_styles_cd)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Content Filtering Section ---
            Text(stringResource(R.string.content_filtering), style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.saveBlurNsfwContent(!blurNsfw) }
                    .padding(vertical = 4.dp)
            ) {
                Text(stringResource(R.string.blur_nsfw_content), modifier = Modifier.weight(1f))
                Switch(
                    checked = blurNsfw,
                    onCheckedChange = { viewModel.saveBlurNsfwContent(it) }
                )
            }
            Text(
                text = stringResource(R.string.blur_nsfw_content_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            // --- Filtered Keywords Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.filtered_keywords), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { viewModel.resetNsfwKeywords() }) {
                    Text(stringResource(R.string.reset_to_default))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    label = { Text(stringResource(R.string.add_keyword)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newKeyword.isNotBlank()) {
                        viewModel.addNsfwKeyword(newKeyword)
                        newKeyword = ""
                    }
                }) {
                    Text(stringResource(R.string.add))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                nsfwKeywords.forEach { keyword ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = keyword, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeNsfwKeyword(keyword) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_keyword))
                        }
                    }
                }
            }
        }
    }
}
