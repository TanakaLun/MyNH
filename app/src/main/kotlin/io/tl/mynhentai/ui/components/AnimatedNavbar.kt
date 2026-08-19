package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

data class NavbarItem(val labelResId: Int, val icon: ImageVector, val route: String)

@Composable
fun AnimatedNavbar(
    items: List<NavbarItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = item.icon,
                label = stringResource(item.labelResId)
            )
        }
    }
}

@Composable
private fun RowScope.NavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label
    )
}