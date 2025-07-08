package jp.kira.sdwebuiremote.ui

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object History : Screen("history")
    object HistoryDetail : Screen("history_detail/{historyId}") {
        fun createRoute(historyId: Int) = "history_detail/$historyId"
    }
    object PngInfo : Screen("png_info")
    object Licenses : Screen("licenses")
    object PromptStyles : Screen("prompt_styles")
    object Queue : Screen("queue")
}