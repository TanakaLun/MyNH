package io.tl.mynhentai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.tl.mynhentai.data.local.SettingsHelper
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun MyNHentaiTheme(
    content: @Composable () -> Unit
) {
    val settings: SettingsHelper = koinInject()
    val monetEnabled by settings.monetEnabledFlow.collectAsState()
    val colorSchemeMode = if (monetEnabled) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    MiuixTheme(
        controller = controller,
        content = content
    )
}