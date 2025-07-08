package jp.kira.sdwebuiremote.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jp.kira.sdwebuiremote.R
import jp.kira.sdwebuiremote.ui.queue.QueueScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showSavePresetDialog by mainViewModel.showSavePresetDialog.collectAsState()
    val showLoadPresetDialog by mainViewModel.showLoadPresetDialog.collectAsState()

    if (showSavePresetDialog) {
        SavePresetDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.showSavePresetDialog.value = false }
        )
    }

    if (showLoadPresetDialog) {
        LoadPresetDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.showLoadPresetDialog.value = false }
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != Screen.Licenses.route,
        drawerContent = {
            AppDrawerContent(navController = navController, closeDrawer = { scope.launch { drawerState.close() } })
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.save_preset)) },
                                    onClick = {
                                        mainViewModel.showSavePresetDialog.value = true
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.load_preset)) },
                                    onClick = {
                                        mainViewModel.showLoadPresetDialog.value = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Main.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Main.route) {
                    MainScreen(viewModel = mainViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPromptStyles = { navController.navigate(Screen.PromptStyles.route) }
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = mainViewModel,
                        navController = navController,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.HistoryDetail.route,
                    arguments = listOf(navArgument("historyId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val historyId = backStackEntry.arguments?.getInt("historyId") ?: 0
                    HistoryDetailScreen(
                        viewModel = mainViewModel,
                        navController = navController,
                        historyId = historyId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.PngInfo.route) {
                    PngInfoScreen()
                }
                composable(Screen.Licenses.route) {
                    LicensesScreen()
                }
                composable(Screen.PromptStyles.route) {
                    PromptStylesScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Queue.route) {
                    QueueScreen()
                }
            }
        }
    }
}

@Composable
fun LicensesScreen() {
    val context = LocalContext.current
    val htmlContent = remember {
        context.resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() }
    }

    AndroidView(
        factory = {
            WebView(it).apply {
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
fun AppDrawerContent(navController: NavController, closeDrawer: () -> Unit) {
    ModalDrawerSheet {
        val context = LocalContext.current
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "N/A"
        }
        Text(
            text = stringResource(R.string.version, versionName ?: "N/A"),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Divider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.generate)) },
            label = { Text(stringResource(R.string.generate)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
                closeDrawer()
            }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history)) },
            label = { Text(stringResource(R.string.history)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.History.route)
                closeDrawer()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.queue)) },
            label = { Text(stringResource(R.string.queue)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.Queue.route)
                closeDrawer()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.png_info)) },
            label = { Text(stringResource(R.string.png_info)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.PngInfo.route)
                closeDrawer()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings)) },
            label = { Text(stringResource(R.string.settings)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.Settings.route)
                closeDrawer()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Description, contentDescription = stringResource(R.string.licenses)) },
            label = { Text(stringResource(R.string.licenses)) },
            selected = false,
            onClick = {
                navController.navigate(Screen.Licenses.route)
                closeDrawer()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = connectionState) {
            is ConnectionState.Disconnected -> {
                Button(onClick = { viewModel.connect() }) {
                    Text(stringResource(R.string.connect_to_server))
                }
            }
            is ConnectionState.Connecting -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.connecting))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.cancelConnection() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
            is ConnectionState.Connected -> {
                GenerationScreen(viewModel = viewModel)
            }
            is ConnectionState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_prefix) + state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.connect() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}