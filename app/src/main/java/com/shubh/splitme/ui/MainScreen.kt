package com.shubh.splitme.ui

import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.OnboardingPreferences
import com.shubh.splitme.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.shubh.splitme.ui.auth.AuthViewModel
import com.shubh.splitme.ui.auth.AuthFlowScreen
import com.shubh.splitme.ui.dashboard.DashboardScreen
import com.shubh.splitme.ui.group.GroupListScreen
import com.shubh.splitme.ui.profile.ProfileScreen
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.LocalBottomBarSpace
import com.shubh.splitme.ui.theme.PureWhite
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.glass
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Groups : Screen("groups")
    object Profile : Screen("profile")

    companion object {
        fun fromRoute(route: String): Screen = when (route) {
            Groups.route -> Groups
            Profile.route -> Profile
            else -> Dashboard
        }
    }
}

private data class TabItem(val screen: Screen, @StringRes val label: Int, val selectedIcon: ImageVector, val icon: ImageVector)

private val Tabs = listOf(
    TabItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TabItem(Screen.Groups, R.string.nav_groups, Icons.Filled.Group, Icons.Outlined.Group),
    TabItem(Screen.Profile, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

private val NavBarShape = RoundedCornerShape(28.dp)

@Composable
fun MainScreen() {
    val app = LocalContext.current.applicationContext as SplitMeApplication
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(app.authRepository))
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Saved by route so the open tab survives the screen being rebuilt (e.g. after changing the
    // app language), instead of jumping back to the Dashboard.
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    val currentScreen = Screen.fromRoute(currentRoute)
    // Last login state we reacted to; null until the first check.
    var handledLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // Intro slides on first launch. null while the saved flag loads, so nothing flashes.
    val context = LocalContext.current
    val onboardingPrefs = remember { OnboardingPreferences(context.applicationContext) }
    val onboardingDone by onboardingPrefs.isDone.map<Boolean, Boolean?> { it }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        // Only on an actual login/logout (or first launch), not on every rebuild of the screen.
        if (handledLoggedIn != isLoggedIn) {
            // After signing in (or out), always start on the Dashboard.
            if (handledLoggedIn != null) currentRoute = Screen.Dashboard.route
            handledLoggedIn = isLoggedIn
        }
    }

    // Onboarding is shown once per install. The flag is saved on this device as soon as the user
    // taps Skip or Get Started, and is never reset (logging out doesn't bring it back). People
    // who are already signed in (e.g. updating from a version without onboarding) skip it.
    LaunchedEffect(onboardingDone, isLoggedIn) {
        if (onboardingDone == false && isLoggedIn) onboardingPrefs.markDone()
    }
    when (onboardingDone) {
        null -> return
        false -> {
            if (!isLoggedIn) OnboardingScreen(onFinish = { scope.launch { onboardingPrefs.markDone() } })
            return
        }
        true -> Unit
    }

    if (!isLoggedIn) {
        AuthFlowScreen(authViewModel)
        return
    }

    val content: @Composable () -> Unit = {
        when (currentScreen) {
            is Screen.Groups -> GroupListScreen()
            is Screen.Profile -> ProfileScreen(
                onBack = { currentRoute = Screen.Dashboard.route },
                onLogout = { authViewModel.logout() }
            )
            else -> DashboardScreen(onNavigateToProfile = { currentRoute = Screen.Profile.route })
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > 600.dp) {
            // Tablets / landscape: side rail, content next to it.
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = glass.surfaceStrong,
                    modifier = Modifier.border(glassBorder(), RectangleShape)
                ) {
                    Tabs.forEach { tab ->
                        val selected = currentScreen == tab.screen
                        NavigationRailItem(
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            selected = selected,
                            onClick = { currentRoute = tab.screen.route },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = PureWhite,
                                indicatorColor = AccentOrange,
                                selectedTextColor = AccentOrange,
                                unselectedIconColor = TextPrimary,
                                unselectedTextColor = TextPrimary
                            )
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            // Phones: a floating glass bar. Screens draw underneath it all the way to the bottom
            // edge, and the bar blurs whatever scrolls behind it.
            val hazeState = remember { HazeState() }
            val density = LocalDensity.current
            var barHeight by remember { mutableStateOf(0.dp) }
            val bottomMargin = 12.dp
            val systemNavBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Box(modifier = Modifier.fillMaxSize()) {
                // Space a screen must leave at its bottom so its last item can scroll clear of the bar.
                CompositionLocalProvider(LocalBottomBarSpace provides barHeight + bottomMargin + systemNavBar + 8.dp) {
                    Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) { content() }
                }

                GlassBottomBar(
                    hazeState = hazeState,
                    currentScreen = currentScreen,
                    onSelect = { currentRoute = it.route },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = systemNavBar + bottomMargin, start = 16.dp, end = 16.dp)
                        .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                )
            }
        }
    }
}

@Composable
private fun GlassBottomBar(
    hazeState: HazeState,
    currentScreen: Screen,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = glass
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassShadow(NavBarShape, blur = 24.dp, offsetY = 8.dp)
            .clip(NavBarShape)
            // Real frosted blur of the content behind (Android 12+); older versions get a solid tint.
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.backdropBottom,
                    tints = listOf(HazeTint(colors.navTint)),
                    blurRadius = 24.dp,
                    noiseFactor = 0.04f,
                    fallbackTint = HazeTint(colors.surfaceStrong)
                )
            )
            .border(glassBorder(), NavBarShape)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(72.dp)
        ) {
            Tabs.forEach { tab ->
                val selected = currentScreen == tab.screen
                NavigationBarItem(
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                    selected = selected,
                    onClick = { onSelect(tab.screen) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        indicatorColor = AccentOrange,
                        selectedTextColor = AccentOrange,
                        unselectedIconColor = TextPrimary,
                        unselectedTextColor = TextPrimary
                    )
                )
            }
        }
    }
}
