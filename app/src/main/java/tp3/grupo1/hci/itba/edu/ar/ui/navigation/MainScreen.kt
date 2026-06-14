package tp3.grupo1.hci.itba.edu.ar.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.screens.dashboard.DashboardScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.DevicesScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms.RoomsScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.routines.RoutinesScreen

enum class MainTab(
    val route: String,
    @field:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.TAB_DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
    DEVICES(Routes.TAB_DEVICES, R.string.nav_devices, Icons.Outlined.Devices),
    ROOMS(Routes.TAB_ROOMS, R.string.nav_rooms, Icons.Outlined.MeetingRoom),
    ROUTINES(Routes.TAB_ROUTINES, R.string.nav_routines, Icons.Outlined.Bolt),
}

/**
 * Shell of the logged-in experience. NavigationSuiteScaffold renders a bottom
 * bar on phones and a navigation rail on tablets / landscape expanded widths
 * (RNF4), while each tab provides its own contextual app bar (RNF2).
 */
@Composable
fun MainScreen(
    onOpenDevice: (String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenHomes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            MainTab.entries.forEach { tab ->
                item(
                    selected = currentRoute == tab.route,
                    onClick = {
                        tabNavController.navigate(tab.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        },
    ) {
        NavHost(
            navController = tabNavController,
            startDestination = Routes.TAB_DASHBOARD,
        ) {
            composable(Routes.TAB_DASHBOARD) {
                DashboardScreen(
                    onOpenDevice = onOpenDevice,
                    onOpenHomes = onOpenHomes,
                    onOpenSettings = onOpenSettings,
                )
            }
            composable(Routes.TAB_DEVICES) {
                DevicesScreen(
                    onOpenDevice = onOpenDevice,
                    onOpenSettings = onOpenSettings,
                )
            }
            composable(Routes.TAB_ROOMS) {
                RoomsScreen(
                    onOpenDevice = onOpenDevice,
                    onOpenRoom = onOpenRoom,
                    onOpenSettings = onOpenSettings,
                )
            }
            composable(Routes.TAB_ROUTINES) {
                RoutinesScreen(
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}
