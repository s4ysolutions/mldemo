package solutions.s4y.mldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import solutions.s4y.mldemo.theme.MLDemoTheme
import solutions.s4y.mldemo.ui.composable.MainDrawer
import solutions.s4y.mldemo.ui.composable.MainTopAppBar
import solutions.s4y.mldemo.ui.composable.navigation.Destinations
import solutions.s4y.mldemo.ui.composable.navigation.MainNavHost
import solutions.s4y.mldemo.ui.composable.navigation.MainNavRouter
import solutions.s4y.mldemo.voice_detection.ui.VoiceDetectionBottomBar

@OptIn(ExperimentalMaterial3Api::class)
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
                    currentNavBackStackEntry?.destination?.route ?: Destinations.Guesser.route
                val mainRouter = remember(navController) {
                    MainNavRouter(navController)
                }

                ModalNavigationDrawer(drawerContent = {
                    MainDrawer(
                        route = currentRoute,
                        navigateToGuesser = { mainRouter.navigateToGuesser() },
                        navigateToVoiceDetection = { mainRouter.navigateToVoiceDetection() },
                        navigateToVoiceTranscription = { mainRouter.navigateToVoiceTranscription() },
                        closeDrawer = { coroutineScope.launch { drawerState.close() } },
                        modifier = Modifier
                    )
                }, drawerState = drawerState) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { MainTopAppBar(onAppMenuClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) },
                        bottomBar = {
                            when(currentRoute) {
                                Destinations.Guesser.route -> {
                                    // GuesserBottomBar()
                                }
                                Destinations.VoiceDetection.route -> {
                                    VoiceDetectionBottomBar()
                                }
                                Destinations.VoiceTranscription.route -> {
                                    // VoiceTranscriptionBottomBar()
                                }
                            }
                        },
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            MainNavHost(navController = navController)
                        }
                    }
                }
            }
        }
    }
}