package jp.kira.sdwebuiremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStoreインスタンスをContextの拡張として定義
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val DEFAULT_NSFW_KEYWORDS = setOf("nsfw", "explicit")
    }

    // 設定項目を識別するためのキー
    private val networkTimeoutKey = intPreferencesKey("network_timeout_seconds")
    private val apiAddressKey = stringPreferencesKey("api_address")
    private val themeSettingKey = stringPreferencesKey("theme_setting")
    private val blurNsfwKey = booleanPreferencesKey("blur_nsfw_content")
    private val nsfwKeywordsKey = stringSetPreferencesKey("nsfw_keywords")

    private val usernameKey = stringPreferencesKey("auth_username")
    private val passwordKey = stringPreferencesKey("auth_password")

    // タイムアウト設定値のFlow
    val timeoutFlow: Flow<Int> = context.dataStore.data
        .map {
            // 値がなければデフォルト値60を返す
            it[networkTimeoutKey] ?: 60
        }

    val apiAddressFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[apiAddressKey] ?: ""
        }

    val themeSettingFlow: Flow<ThemeSetting> = context.dataStore.data
        .map {
            ThemeSetting.valueOf(it[themeSettingKey] ?: ThemeSetting.System.name)
        }

    val blurNsfwContentFlow: Flow<Boolean> = context.dataStore.data
        .map {
            it[blurNsfwKey] ?: true // Default to true (blur)
        }

    val nsfwKeywordsFlow: Flow<Set<String>> = context.dataStore.data
        .map {
            it[nsfwKeywordsKey] ?: DEFAULT_NSFW_KEYWORDS
        }

    val usernameFlow: Flow<String> = context.dataStore.data
        .map {
            it[usernameKey] ?: ""
        }

    val passwordFlow: Flow<String> = context.dataStore.data
        .map {
            it[passwordKey] ?: ""
        }

    // タイムアウト値を保存��るためのsuspend関数
    suspend fun saveTimeout(timeout: Int) {
        context.dataStore.edit {
            it[networkTimeoutKey] = timeout
        }
    }

    suspend fun saveApiAddress(address: String) {
        context.dataStore.edit {
            it[apiAddressKey] = address
        }
    }

    suspend fun saveThemeSetting(theme: ThemeSetting) {
        context.dataStore.edit {
            it[themeSettingKey] = theme.name
        }
    }

    suspend fun saveBlurNsfwContent(shouldBlur: Boolean) {
        context.dataStore.edit {
            it[blurNsfwKey] = shouldBlur
        }
    }

    suspend fun saveNsfwKeywords(keywords: Set<String>) {
        context.dataStore.edit {
            it[nsfwKeywordsKey] = keywords
        }
    }

    suspend fun saveCredentials(username: String, password: String) {
        context.dataStore.edit {
            it[usernameKey] = username
            it[passwordKey] = password
        }
    }
}
