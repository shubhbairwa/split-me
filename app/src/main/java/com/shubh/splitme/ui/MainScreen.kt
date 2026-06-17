package com.shubh.splitme.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.ui.auth.AuthViewModel
import com.shubh.splitme.ui.auth.LoginScreen
import com.shubh.splitme.ui.auth.SignupScreen
import com.shubh.splitme.ui.dashboard.DashboardScreen
import com.shubh.splitme.ui.group.GroupListScreen
import com.shubh.splitme.ui.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
    object Groups : Screen("groups")
    object Profile : Screen("profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val app = LocalContext.current.applicationContext as SplitMeApplication
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(app.authRepository))
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    LaunchedEffect(isLoggedIn) {
        currentScreen = if (isLoggedIn) Screen.Dashboard else Screen.Login
    }

    if (!isLoggedIn) {
        when (currentScreen) {
            is Screen.Signup -> SignupScreen(onLoginClick = { currentScreen = Screen.Login })
            else -> LoginScreen(onSignupClick = { currentScreen = Screen.Signup })
        }
    } else {
        BoxWithConstraints {
            val isWide = maxWidth > 600.dp
            
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationRailItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Dashboard") },
                            selected = currentScreen == Screen.Dashboard,
                            onClick = { currentScreen = Screen.Dashboard }
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Default.Group, contentDescription = null) },
                            label = { Text("Groups") },
                            selected = currentScreen == Screen.Groups,
                            onClick = { currentScreen = Screen.Groups }
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Profile") },
                            selected = currentScreen == Screen.Profile,
                            onClick = { currentScreen = Screen.Profile }
                        )
                    }
                }
                
                Scaffold(
                    bottomBar = {
                        if (!isWide) {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                    label = { Text("Dashboard") },
                                    selected = currentScreen == Screen.Dashboard,
                                    onClick = { currentScreen = Screen.Dashboard }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    label = { Text("Groups") },
                                    selected = currentScreen == Screen.Groups,
                                    onClick = { currentScreen = Screen.Groups }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    label = { Text("Profile") },
                                    selected = currentScreen == Screen.Profile,
                                    onClick = { currentScreen = Screen.Profile }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            is Screen.Dashboard -> DashboardScreen(onNavigateToProfile = { currentScreen = Screen.Profile })
                            is Screen.Groups -> GroupListScreen()
                            is Screen.Profile -> ProfileScreen(
                                onBack = { currentScreen = Screen.Dashboard },
                                onLogout = { authViewModel.logout() }
                            )
                            else -> DashboardScreen(onNavigateToProfile = { currentScreen = Screen.Profile })
                        }
                    }
                }
            }
        }
    }
}
