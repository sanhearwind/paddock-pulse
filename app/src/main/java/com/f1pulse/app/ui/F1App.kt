package com.f1pulse.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.f1pulse.app.data.prefs.ThemeMode
import com.f1pulse.app.ui.components.RacingGlassBackground
import com.f1pulse.app.ui.nav.F1BottomBar
import com.f1pulse.app.ui.nav.F1NavHost
import com.f1pulse.app.ui.nav.Route
import com.f1pulse.app.ui.nav.navigateToTab
import com.f1pulse.app.ui.settings.SettingsViewModel
import com.f1pulse.app.ui.theme.F1Theme

/**
 * Root composable. Reads user settings (theme mode) and hosts the navigation graph.
 * The bottom bar is shown only on top-level destinations.
 */
@Composable
fun F1App() {
    val settingsVm: SettingsViewModel = hiltViewModel()
    val settings by settingsVm.settings.collectAsStateWithLifecycle(initialValue = null)
    val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM

    F1Theme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            val appContent: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize()) {
                    F1NavHost(navController, Modifier.fillMaxSize())
                    if (currentRoute in topLevelRoutes()) {
                        F1BottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigateToTab(route)
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }

            if (currentRoute in topLevelRoutes()) {
                RacingGlassBackground(Modifier.fillMaxSize()) {
                    appContent()
                }
            } else {
                appContent()
            }
        }
    }
}

private fun topLevelRoutes(): Set<String> = setOf(
    Route.Home.route,
    Route.Calendar.route,
    Route.History.route,
    Route.Standings.route,
    Route.Settings.route,
)
