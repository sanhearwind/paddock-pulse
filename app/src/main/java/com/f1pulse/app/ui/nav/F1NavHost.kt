package com.f1pulse.app.ui.nav

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.f1pulse.app.R
import com.f1pulse.app.ui.calendar.CalendarScreen
import com.f1pulse.app.ui.constructordetail.ConstructorDetailScreen
import com.f1pulse.app.ui.driverdetail.DriverDetailScreen
import com.f1pulse.app.ui.home.HomeScreen
import com.f1pulse.app.ui.history.HistoryScreen
import com.f1pulse.app.ui.components.RacingGlassSurface
import com.f1pulse.app.ui.racedetail.RaceDetailScreen
import com.f1pulse.app.ui.settings.SettingsScreen
import com.f1pulse.app.ui.standings.StandingsScreen
import com.f1pulse.app.ui.standings.StandingsTab
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.widget.FavoriteDriverConfigScreen
import com.f1pulse.app.ui.widget.FavoriteTeamConfigScreen
import com.f1pulse.app.ui.widget.WidgetPickerScreen

/** All navigation routes used across the app. */
sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Calendar : Route("calendar")
    data object History : Route("history")
    data object Standings : Route("standings")
    data object Settings : Route("settings")
    data object RaceDetail : Route("raceDetail/{season}/{round}") {
        fun build(season: Int, round: Int) = "raceDetail/$season/$round"
    }
    data object DriverDetail : Route("driverDetail/{season}/{driverId}") {
        fun build(season: Int, driverId: String) = "driverDetail/$season/$driverId"
    }
    data object ConstructorDetail : Route("constructorDetail/{season}/{constructorId}") {
        fun build(season: Int, constructorId: String) = "constructorDetail/$season/$constructorId"
    }
    data object WidgetPicker : Route("widgetPicker")
    data object FavoriteTeamConfig : Route("favoriteTeamConfig")
    data object FavoriteDriverConfig : Route("favoriteDriverConfig")
}

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(Route.Home.route, R.string.nav_home, Icons.Filled.Home),
    BottomNavItem(Route.Calendar.route, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    BottomNavItem(Route.History.route, R.string.nav_history, Icons.Filled.History),
    BottomNavItem(Route.Standings.route, R.string.nav_standings, Icons.Filled.EmojiEvents),
    BottomNavItem(Route.Settings.route, R.string.nav_settings, Icons.Filled.Settings),
)

/** Tab route order — used to determine slide direction when switching bottom-nav tabs. */
private val ApplePageEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private val tabRouteOrder = listOf(
    Route.Home.route,
    Route.Calendar.route,
    Route.History.route,
    Route.Standings.route,
    Route.Settings.route,
)

/** Returns the tab index for a route, or Int.MAX_VALUE if it's not a tab route (detail screen). */
private fun tabRouteIndex(route: String?): Int {
    val base = route?.substringBefore("/")
    val idx = tabRouteOrder.indexOf(base)
    return if (idx == -1) Int.MAX_VALUE else idx
}

/**
 * 统一的底部导航栏切换逻辑。所有 tab 级别页面的跳转都应使用此方法，
 * 确保返回栈一致：popUpTo 起始页（保留），saveState/restoreState 保存状态，
 * launchSingleTop 防止重复入栈。
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private const val CALENDAR_TAB_KEY = "calendar_tab"
private const val STANDINGS_TAB_KEY = "standings_tab"

private fun NavHostController.navigateToCalendarTab(index: Int) {
    navigateToTab(Route.Calendar.route)
    currentBackStackEntry?.savedStateHandle?.set(CALENDAR_TAB_KEY, index.coerceIn(0, 1))
}

private fun NavHostController.navigateToStandingsTab(tab: StandingsTab) {
    navigateToTab(Route.Standings.route)
    currentBackStackEntry?.savedStateHandle?.set(STANDINGS_TAB_KEY, tab.name)
}

@Composable
fun F1BottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(F1BottomBarLayout.Height)
            .offset(y = -F1BottomBarLayout.Lift)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        RacingGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            fillAlphaMultiplier = 0.66f,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val blurredFrost = if (dark) {
                    listOf(
                        Color(0xFF17171F).copy(alpha = 0.82f),
                        F1Red.copy(alpha = 0.24f),
                        Color(0xFF33496F).copy(alpha = 0.24f),
                        Color(0xFF111116).copy(alpha = 0.78f),
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.88f),
                        F1Red.copy(alpha = 0.15f),
                        Color(0xFFB7D8F5).copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.82f),
                    )
                }

                // Blur only this decorative frost layer. The page tree, nav icons,
                // labels and selection indicator stay outside the RenderEffect.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(16.dp)
                        .background(Brush.horizontalGradient(blurredFrost)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (dark) {
                                Color.Black.copy(alpha = 0.12f)
                            } else {
                                Color.White.copy(alpha = 0.22f)
                            },
                        ),
                )

                val selectedIndex = bottomNavItems
                    .indexOfFirst { it.route == currentRoute }
                    .coerceAtLeast(0)
                val itemWidth = maxWidth / bottomNavItems.size
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "bottomNavIndicator",
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .background(
                            color = F1Red.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(24.dp),
                        ),
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    bottomNavItems.forEach { item ->
                        AppleStyleNavItem(
                            modifier = Modifier.weight(1f),
                            item = item,
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleStyleNavItem(
    modifier: Modifier,
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            selected -> 1.04f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bottomNavScale",
    )
    val iconOffset by animateDpAsState(
        targetValue = if (selected) (-1).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bottomNavIconOffset",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) F1Red else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "bottomNavColor",
    )
    val label = stringResource(item.labelRes)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .offset(y = iconOffset)
                .size(25.dp),
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * Root navigation graph. Hosts the five top-level tabs plus detail/configuration screens.
 *
 * Tab switches use directional slide animation: navigating right slides the new
 * screen in from the right; navigating left slides it in from the left.
 * Detail screen pushes always slide in from the right; pops slide out to the right.
 */
@Composable
fun F1NavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier,
        enterTransition = {
            val targetIdx = tabRouteIndex(targetState.destination.route)
            val currentIdx = tabRouteIndex(initialState.destination.route)
            val tabSwitch = targetIdx != Int.MAX_VALUE && currentIdx != Int.MAX_VALUE
            val initialOffset: (Int) -> Int = when {
                !tabSwitch -> ({ width -> width })
                targetIdx > currentIdx -> ({ width -> width / 7 })
                else -> ({ width -> -width / 7 })
            }
            slideInHorizontally(
                animationSpec = tween(
                    durationMillis = if (tabSwitch) 220 else 310,
                    easing = ApplePageEasing,
                ),
                initialOffsetX = initialOffset,
            ) + fadeIn(animationSpec = tween(durationMillis = 170))
        },
        exitTransition = {
            val targetIdx = tabRouteIndex(targetState.destination.route)
            val currentIdx = tabRouteIndex(initialState.destination.route)
            val tabSwitch = targetIdx != Int.MAX_VALUE && currentIdx != Int.MAX_VALUE
            val targetOffset: (Int) -> Int = when {
                !tabSwitch -> ({ width -> -width / 5 })
                targetIdx > currentIdx -> ({ width -> -width / 14 })
                else -> ({ width -> width / 14 })
            }
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = if (tabSwitch) 150 else 260,
                    easing = ApplePageEasing,
                ),
                targetOffsetX = targetOffset,
            ) + fadeOut(animationSpec = tween(durationMillis = 130))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 280, easing = ApplePageEasing),
            ) { -it / 5 } + fadeIn(animationSpec = tween(durationMillis = 170))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 310, easing = ApplePageEasing),
            ) { it } + fadeOut(animationSpec = tween(durationMillis = 140))
        },
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToStandings = { navController.navigateToTab(Route.Standings.route) },
                onNavigateToSettings = { navController.navigateToTab(Route.Settings.route) },
                onNavigateToRaceDetail = { season, round ->
                    navController.navigate(Route.RaceDetail.build(season, round))
                },
                onNavigateToDriverDetail = { season, driverId ->
                    navController.navigate(Route.DriverDetail.build(season, driverId))
                },
                onSwipeLeft = { navController.navigateToCalendarTab(0) },
            )
        }
        composable(Route.Calendar.route) { entry ->
            val tabIndex by entry.savedStateHandle
                .getStateFlow(CALENDAR_TAB_KEY, 0)
                .collectAsStateWithLifecycle()
            CalendarScreen(
                onNavigateToRaceDetail = { season, round ->
                    navController.navigate(Route.RaceDetail.build(season, round))
                },
                selectedTabIndex = tabIndex,
                onSelectedTabChange = { entry.savedStateHandle[CALENDAR_TAB_KEY] = it },
                onNavigatePrevious = { navController.navigateToTab(Route.Home.route) },
                onNavigateNext = { navController.navigateToTab(Route.History.route) },
            )
        }
        composable(Route.History.route) {
            HistoryScreen(
                onNavigateToRaceDetail = { season, round ->
                    navController.navigate(Route.RaceDetail.build(season, round))
                },
                onNavigatePrevious = { navController.navigateToCalendarTab(1) },
                onNavigateNext = { navController.navigateToStandingsTab(StandingsTab.DRIVERS) },
            )
        }
        composable(Route.Standings.route) { entry ->
            val tabName by entry.savedStateHandle
                .getStateFlow(STANDINGS_TAB_KEY, StandingsTab.DRIVERS.name)
                .collectAsStateWithLifecycle()
            val selectedTab = runCatching { StandingsTab.valueOf(tabName) }
                .getOrDefault(StandingsTab.DRIVERS)
            StandingsScreen(
                onNavigateToDriverDetail = { season, driverId ->
                    navController.navigate(Route.DriverDetail.build(season, driverId))
                },
                onNavigateToConstructorDetail = { season, constructorId ->
                    navController.navigate(Route.ConstructorDetail.build(season, constructorId))
                },
                selectedTab = selectedTab,
                onSelectedTabChange = { entry.savedStateHandle[STANDINGS_TAB_KEY] = it.name },
                onNavigatePrevious = { navController.navigateToTab(Route.History.route) },
                onNavigateNext = { navController.navigateToTab(Route.Settings.route) },
            )
        }
        composable(Route.Settings.route) {
            SettingsScreen(
                onNavigateToWidgetPicker = { navController.navigate(Route.WidgetPicker.route) },
                onNavigateToFavoriteTeamConfig = { navController.navigate(Route.FavoriteTeamConfig.route) },
                onNavigateToFavoriteDriverConfig = { navController.navigate(Route.FavoriteDriverConfig.route) },
                onSwipeRight = { navController.navigateToStandingsTab(StandingsTab.CONSTRUCTORS) },
            )
        }
        composable(
            route = Route.RaceDetail.route,
            arguments = listOf(
                navArgument("season") { type = NavType.IntType },
                navArgument("round") { type = NavType.IntType },
            ),
        ) {
            RaceDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.DriverDetail.route,
            arguments = listOf(
                navArgument("season") { type = NavType.IntType },
                navArgument("driverId") { type = NavType.StringType },
            ),
        ) {
            DriverDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToConstructorDetail = { season, constructorId ->
                    navController.navigate(Route.ConstructorDetail.build(season, constructorId))
                },
            )
        }
        composable(
            route = Route.ConstructorDetail.route,
            arguments = listOf(
                navArgument("season") { type = NavType.IntType },
                navArgument("constructorId") { type = NavType.StringType },
            ),
        ) {
            ConstructorDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDriverDetail = { season, driverId ->
                    navController.navigate(Route.DriverDetail.build(season, driverId))
                },
            )
        }
        composable(Route.WidgetPicker.route) {
            WidgetPickerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToFavoriteTeamConfig = { navController.navigate(Route.FavoriteTeamConfig.route) },
                onNavigateToFavoriteDriverConfig = { navController.navigate(Route.FavoriteDriverConfig.route) },
            )
        }
        composable(Route.FavoriteTeamConfig.route) {
            FavoriteTeamConfigScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.FavoriteDriverConfig.route) {
            FavoriteDriverConfigScreen(onBack = { navController.popBackStack() })
        }
    }
}
