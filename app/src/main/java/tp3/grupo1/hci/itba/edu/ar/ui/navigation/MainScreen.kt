package tp3.grupo1.hci.itba.edu.ar.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import tp3.grupo1.hci.itba.edu.ar.ui.screens.statistics.StatisticsScreen

enum class MainTab(
    val route: String,
    @field:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.TAB_DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
    ROOMS(Routes.TAB_ROOMS, R.string.nav_rooms, Icons.Outlined.MeetingRoom),
    DEVICES(Routes.TAB_DEVICES, R.string.nav_devices, Icons.Outlined.Devices),
    ROUTINES(Routes.TAB_ROUTINES, R.string.nav_routines, Icons.Outlined.Schedule),
    STATISTICS(Routes.TAB_STATISTICS, R.string.nav_statistics, Icons.Outlined.BarChart),
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
    onOpenNotifications: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // One-shot filter handed to the Devices tab when opened from a dashboard
    // section ("Cerraduras", "Alarmas"); consumed on arrival.
    var pendingDevicesFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Pinea explicitamente el layoutType: en compact ancho usa NavigationBar
    // (bottom), en medium/expanded usa NavigationRail (lateral). Es lo que ya
    // hace el default pero al hacerlo explicito Material3 1.3 propaga insets
    // de forma consistente y permite a los Scaffold internos zero-ear su
    // contentWindowInsets sin perder padding.
    val adaptiveInfo = currentWindowAdaptiveInfo()
    NavigationSuiteScaffold(
        layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo),
        navigationSuiteItems = {
            MainTab.entries.forEach { tab ->
                item(
                    selected = currentRoute?.substringBefore("?") == tab.route,
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
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            // Smaller so long labels ("Estadísticas") fit on one line.
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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
                    onOpenRoom = onOpenRoom,
                    onOpenHomes = onOpenHomes,
                    onOpenSettings = onOpenSettings,
                    onOpenNotifications = onOpenNotifications,
                    onOpenRooms = {
                        tabNavController.navigate(Routes.TAB_ROOMS) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenDevices = {
                        tabNavController.navigateToDevices()
                    },
                    onOpenLocks = {
                        pendingDevicesFilter = "locks"
                        tabNavController.navigateToDevices()
                    },
                    onOpenAlarms = {
                        pendingDevicesFilter = "alarms"
                        tabNavController.navigateToDevices()
                    },
                )
            }
            composable(Routes.TAB_DEVICES) {
                DevicesScreen(
                    onOpenDevice = onOpenDevice,
                    onOpenSettings = onOpenSettings,
                    initialFilter = pendingDevicesFilter,
                    onFilterConsumed = { pendingDevicesFilter = null },
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
                    onCreateRoutine = onCreateRoutine,
                    onEditRoutine = onEditRoutine,
                )
            }
            composable(Routes.TAB_STATISTICS) {
                StatisticsScreen(
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

/** Switch to the Devices tab, optionally carrying an initial filter route. */
private fun androidx.navigation.NavController.navigateToDevices(
    route: String = Routes.TAB_DEVICES,
) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
