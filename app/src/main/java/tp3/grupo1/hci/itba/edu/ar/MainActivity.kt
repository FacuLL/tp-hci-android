package tp3.grupo1.hci.itba.edu.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tp3.grupo1.hci.itba.edu.ar.data.ThemeMode
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.navigation.MainScreen
import tp3.grupo1.hci.itba.edu.ar.ui.navigation.Routes
import tp3.grupo1.hci.itba.edu.ar.ui.screens.auth.ForgotPasswordScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.auth.LoginScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.auth.RegisterScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.auth.VerifyAccountScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.DeviceDetailScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.homes.HomesScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms.RoomDetailScreen
import tp3.grupo1.hci.itba.edu.ar.ui.screens.settings.SettingsScreen
import tp3.grupo1.hci.itba.edu.ar.ui.theme.LuminaTheme
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LuminaApplication).container
        setContent {
            val themeMode by container.preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LuminaTheme(darkTheme = darkTheme) {
                LuminaApp(container)
            }
        }
    }
}

@Composable
fun LuminaApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val initialized by container.sessionManager.initialized.collectAsStateWithLifecycle()
    val token by container.sessionManager.token.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // RF20: device events arrive as system notifications, so ask for the
    // permission once a session exists (required from API 33).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(token) {
        if (token != null && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val sessionExpiredMessage = stringResource(R.string.error_session_expired)
    LaunchedEffect(Unit) {
        container.sessionManager.sessionExpired.collect {
            snackbarHostState.showSnackbar(sessionExpiredMessage)
        }
    }

    // When the session disappears (logout or expiry) the user goes back to login.
    LaunchedEffect(token, initialized) {
        if (!initialized) return@LaunchedEffect
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val inAuthFlow = currentRoute == null ||
            currentRoute == Routes.LOGIN ||
            currentRoute == Routes.REGISTER ||
            currentRoute == Routes.VERIFY ||
            currentRoute == Routes.FORGOT_PASSWORD
        if (token == null && !inAuthFlow) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    if (!initialized) {
        CenteredLoading()
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Only the top inset is applied here; bottom insets are handled by
        // each screen's own bars so the navigation bar reaches the edge.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding())
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (token != null) Routes.MAIN else Routes.LOGIN,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    onNeedsVerification = { email -> navController.navigate(Routes.verify(email)) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { email -> navController.navigate(Routes.verify(email)) },
                    onNavigateUp = { navController.navigateUp() },
                )
            }
            composable(
                route = Routes.VERIFY,
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { entry ->
                val email = URLDecoder.decode(entry.arguments?.getString("email").orEmpty(), "UTF-8")
                VerifyAccountScreen(
                    email = email,
                    onVerified = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateUp = { navController.navigateUp() },
                )
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onDone = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateUp = { navController.navigateUp() },
                )
            }
            composable(Routes.MAIN) {
                MainScreen(
                    onOpenDevice = { deviceId -> navController.navigate(Routes.deviceDetail(deviceId)) },
                    onOpenRoom = { roomId -> navController.navigate(Routes.roomDetail(roomId)) },
                    onOpenHomes = { navController.navigate(Routes.HOMES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                route = Routes.DEVICE_DETAIL,
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
            ) { entry ->
                DeviceDetailScreen(
                    deviceId = entry.arguments?.getString("deviceId").orEmpty(),
                    onNavigateUp = { navController.navigateUp() },
                )
            }
            composable(
                route = Routes.ROOM_DETAIL,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            ) { entry ->
                RoomDetailScreen(
                    roomId = entry.arguments?.getString("roomId").orEmpty(),
                    onNavigateUp = { navController.navigateUp() },
                    onOpenDevice = { deviceId -> navController.navigate(Routes.deviceDetail(deviceId)) },
                )
            }
            composable(Routes.HOMES) {
                HomesScreen(onNavigateUp = { navController.navigateUp() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onNavigateUp = { navController.navigateUp() })
            }
        }
    }
}
