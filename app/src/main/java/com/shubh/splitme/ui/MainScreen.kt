package com.shubh.splitme.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shubh.splitme.ui.dashboard.DashboardScreen
import com.shubh.splitme.ui.group.GroupListScreen
import com.shubh.splitme.ui.member.MemberListScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Groups : Screen("groups")
    object Members : Screen("members")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    
    BoxWithConstraints {
        val isWide = maxWidth > 600.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                NavigationRail {
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
                        label = { Text("Members") },
                        selected = currentScreen == Screen.Members,
                        onClick = { currentScreen = Screen.Members }
                    )
                }
            }
            
            Scaffold(
                bottomBar = {
                    if (!isWide) {
                        NavigationBar {
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
                                label = { Text("Members") },
                                selected = currentScreen == Screen.Members,
                                onClick = { currentScreen = Screen.Members }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        is Screen.Dashboard -> DashboardScreen()
                        is Screen.Groups -> GroupListScreen()
                        is Screen.Members -> MemberListScreen()
                    }
                }
            }
        }
    }
}
