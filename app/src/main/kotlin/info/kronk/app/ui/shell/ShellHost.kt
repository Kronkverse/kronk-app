package info.kronk.app.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.kronk.core.designsystem.primitive.BottomTabBar
import info.kronk.core.designsystem.primitive.BottomTabItem
import info.kronk.feature.home.ui.HomeScreen

// Top-level app scaffold after sign-in. Owns:
//   - the NavHost (one destination per PillarKey),
//   - the BottomTabBar wired to the current back-stack entry,
//   - the safe-area padding around the primary content zone.
//
// Nav choice — bottom-bar tabs use `launchSingleTop` + `saveState` /
// `restoreState` so re-tapping a tab returns to that tab's back stack
// intact rather than pushing another copy. `popUpTo(startDestination)`
// keeps the back stack shallow so the system Back button always leads
// out through Home, not through a chain of tab switches.

@Composable
fun ShellHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PillarKey.Home.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val items = PillarKey.values().map { pillar ->
                BottomTabItem(
                    icon = painterResource(pillar.iconRes),
                    contentDescription = stringResource(pillar.labelRes),
                    selected = currentRoute == pillar.route,
                    onSelect = {
                        if (currentRoute != pillar.route) {
                            navController.navigate(pillar.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
            BottomTabBar(items = items)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PillarKey.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(PillarKey.Me.route) { PlaceholderScreen(PillarKey.Me) }
            composable(PillarKey.Home.route) { HomeScreen() }
            composable(PillarKey.Awawb.route) { PlaceholderScreen(PillarKey.Awawb) }
            composable(PillarKey.Hub.route) { PlaceholderScreen(PillarKey.Hub) }
            composable(PillarKey.Nudges.route) { PlaceholderScreen(PillarKey.Nudges) }
        }
    }
}

