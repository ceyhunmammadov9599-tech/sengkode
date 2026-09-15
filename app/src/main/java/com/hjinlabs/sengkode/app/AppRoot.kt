package com.hjinlabs.sengkode.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hjinlabs.sengkode.feature.generator.GeneratorScreen
import com.hjinlabs.sengkode.feature.history.HistoryScreen
import com.hjinlabs.sengkode.feature.templates.TemplatesScreen

private data class Destination(
    val route: Any,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val destinations = listOf(
    Destination(GeneratorRoute, R.string.tab_generator, Icons.Outlined.QrCode2),
    Destination(HistoryRoute, R.string.tab_history, Icons.Outlined.History),
    Destination(TemplatesRoute, R.string.tab_templates, Icons.Outlined.Style),
)

/**
 * Single-Activity navigation root: one NavHost, three bottom destinations,
 * state restoration handled by Navigation Compose.
 */
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route::class.qualifiedName
                        } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = GeneratorRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<GeneratorRoute> { GeneratorScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<TemplatesRoute> { TemplatesScreen() }
        }
    }
}
