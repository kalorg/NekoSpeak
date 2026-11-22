package com.nekospeak.tts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nekospeak.tts.data.PrefsManager
import com.nekospeak.tts.ui.navigation.Screen
import com.nekospeak.tts.ui.screens.SettingsScreen
import com.nekospeak.tts.ui.screens.VoicesScreen
import com.nekospeak.tts.ui.screens.VoiceRecorderScreen
import com.nekospeak.tts.ui.screens.OnboardingScreen
import com.nekospeak.tts.ui.screens.ModelManagerScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    
    val startDestination = if (prefs.isOnboardingComplete) Screen.Voices.route else Screen.Onboarding.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hide bottom bar on certain screens
    val hideBottomBarRoutes = listOf(
        Screen.Onboarding.route,
        Screen.VoiceRecorder.route,
        Screen.ModelManager.route
    )
    val showBottomBar = currentRoute !in hideBottomBarRoutes
    
    // State for voice cloning callback (Path, Name, Transcript)
    var pendingVoiceCloneData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController)
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            EngineStateBanner()
            NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }
            
            composable(Screen.Voices.route) {
                VoicesScreen(
                    navController = navController,
                    pendingVoiceCloneData = pendingVoiceCloneData,
                    onVoiceCloneHandled = { pendingVoiceCloneData = null }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            
            composable(Screen.VoiceRecorder.route) {
                VoiceRecorderScreen(
                    navController = navController,
                    onVoiceRecorded = { path: String, name: String, transcript: String ->
                        pendingVoiceCloneData = Triple(path, name, transcript)
                    }
                )
            }
            
            composable(Screen.ModelManager.route) {
                com.nekospeak.tts.ui.screens.ModelManagerScreen(navController = navController)
            }
        }
        }
    }
}

/**
 * Bandeau d'état du moteur TTS : visible pendant l'initialisation (progression),
 * 5 secondes après le passage en « prêt », et en cas d'échec (persistant).
 * Alimenté par NekoTtsService via EngineInitState.
 */
@Composable
fun EngineStateBanner() {
    val state by com.nekospeak.tts.data.EngineInitState.state.collectAsState()
    var showReady by remember { mutableStateOf(false) }

    // « Moteur prêt » reste affiché 5 s puis s'efface
    LaunchedEffect(state.changedAt) {
        if (state.status == com.nekospeak.tts.data.EngineInitState.Status.READY) {
            showReady = true
            kotlinx.coroutines.delay(5000)
            showReady = false
        } else {
            showReady = false
        }
    }

    val visible = when (state.status) {
        com.nekospeak.tts.data.EngineInitState.Status.INITIALIZING -> true
        com.nekospeak.tts.data.EngineInitState.Status.FAILED -> true
        com.nekospeak.tts.data.EngineInitState.Status.READY -> showReady
        com.nekospeak.tts.data.EngineInitState.Status.IDLE -> false
    }

    AnimatedVisibility(visible = visible) {
        Surface(
            color = when (state.status) {
                com.nekospeak.tts.data.EngineInitState.Status.FAILED ->
                    MaterialTheme.colorScheme.errorContainer
                com.nekospeak.tts.data.EngineInitState.Status.READY ->
                    Color(0xFF1B5E20)
                else -> Color(0xFF37474F)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (state.status) {
                        com.nekospeak.tts.data.EngineInitState.Status.READY ->
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = Color.White)
                        com.nekospeak.tts.data.EngineInitState.Status.FAILED ->
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer)
                        else -> androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(18.dp)
                        )
                    }
                    Text(
                        text = when (state.status) {
                            com.nekospeak.tts.data.EngineInitState.Status.INITIALIZING ->
                                "Préparation du moteur (${state.model})…"
                            com.nekospeak.tts.data.EngineInitState.Status.READY ->
                                "Moteur prêt (${state.model})"
                            com.nekospeak.tts.data.EngineInitState.Status.FAILED ->
                                "Échec d'initialisation du moteur" +
                                    (state.message?.let { " : $it" } ?: "")
                            else -> ""
                        },
                        color = if (state.status == com.nekospeak.tts.data.EngineInitState.Status.FAILED)
                            MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (state.status == com.nekospeak.tts.data.EngineInitState.Status.INITIALIZING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val items = listOf(
            Screen.Voices to Icons.AutoMirrored.Filled.List,
            Screen.Settings to Icons.Default.Settings
        )

        items.forEach { (screen, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
