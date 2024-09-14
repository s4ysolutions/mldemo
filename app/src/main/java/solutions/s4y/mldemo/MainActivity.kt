package solutions.s4y.mldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import solutions.s4y.agora.ui.AgoraBottomBar
import solutions.s4y.agora.ui.AgoraScreen
import solutions.s4y.mldemo.asr.ui.ASRBottomBar
import solutions.s4y.mldemo.asr.ui.VoiceTranscriptionScreen
import solutions.s4y.mldemo.guesser.GuesserScreen
import solutions.s4y.mldemo.theme.MLDemoTheme
import solutions.s4y.mldemo.ui.composable.MainDrawer
import solutions.s4y.mldemo.ui.composable.MainTopAppBar
import solutions.s4y.mldemo.ui.composable.navigation.Destinations
import solutions.s4y.mldemo.voice_detection.ui.VoiceDetectionBottomBar
import solutions.s4y.mldemo.voice_detection.ui.VoiceDetectionScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MLDemoTheme {
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val navController = rememberNavController()
                val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute =
                    currentNavBackStackEntry?.destination?.route ?: Destinations.defaultRoute.route

                ModalNavigationDrawer(drawerContent = {
                    MainDrawer(
                        navController = navController,
                        modifier = Modifier,
                        closeDrawer = { coroutineScope.launch { drawerState.close() } },
                    )
                }, drawerState = drawerState) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            MainTopAppBar(onAppMenuClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            })
                        },
                        bottomBar = {
                            when (currentRoute) {
                                Destinations.Guesser.route -> {
                                    // GuesserBottomBar()
                                }

                                Destinations.VoiceClassification.route -> {
                                    VoiceDetectionBottomBar()
                                }

                                Destinations.ASR.route -> {
                                    ASRBottomBar()
                                }

                                Destinations.Agora.route -> {
                                    AgoraBottomBar()
                                }
                            }
                        },
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(navController, startDestination = Destinations.defaultRoute.route) {
                                composable(Destinations.VoiceClassification.route) {
                                    VoiceDetectionScreen()
                                }
                                composable(Destinations.ASR.route) {
                                    VoiceTranscriptionScreen()
                                }
                                composable(Destinations.Guesser.route) {
                                    GuesserScreen()
                                }
                                composable(Destinations.Agora.route) {
                                    AgoraScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
