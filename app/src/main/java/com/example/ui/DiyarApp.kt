package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddHomeWork
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.repository.DiyarRepository
import com.example.data.repository.PreferencesManager
import com.example.i18n.AppLanguage
import com.example.i18n.AppThemeMode
import com.example.i18n.ArabicStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalStrings
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.PublishScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.VirtualTourScreen
import com.example.ui.theme.DiyarTheme
import com.example.ui.viewmodel.DiyarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

object DiyarRoutes {
    const val DISCOVER = "discover"
    const val FAVORITES = "favorites"
    const val PUBLISH = "publish"
    const val SERVICES = "services"
    const val PROFILE = "profile"
    const val ALERTS = "alerts"
    const val DETAIL = "detail/{propertyId}"
    const val TOUR_360 = "tour/{propertyId}"

    fun detail(propertyId: String) = "detail/$propertyId"
    fun tour(propertyId: String) = "tour/$propertyId"
}

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun DiyarApp(
    deepLinkPropertyId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesManager(context.applicationContext) }
    val language by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.AR)
    val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)

    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isArabic = language == AppLanguage.AR
    val strings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Sync JVM/Android Locale cleanly whenever language changes
    LaunchedEffect(language) {
        Locale.setDefault(Locale(language.code))
    }

    val toggleLanguage: () -> Unit = {
        scope.launch {
            prefs.setLanguage(if (isArabic) AppLanguage.EN else AppLanguage.AR)
        }
    }
    val toggleTheme: () -> Unit = {
        scope.launch {
            prefs.setThemeMode(if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK)
        }
    }
    val setThemeMode: (AppThemeMode) -> Unit = { mode ->
        scope.launch {
            prefs.setThemeMode(mode)
        }
    }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDir
    ) {
        DiyarTheme(darkTheme = isDark, isArabic = isArabic) {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            val user = currentUser
            if (user == null) {
                AuthScreen(
                    isDark = isDark,
                    onToggleLanguage = toggleLanguage,
                    onToggleTheme = toggleTheme,
                    onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    AuthenticatedDiyarShell(
                        currentUserId = uid,
                        language = language,
                        themeMode = themeMode,
                        isDark = isDark,
                        deepLinkPropertyId = deepLinkPropertyId,
                        onDeepLinkConsumed = onDeepLinkConsumed,
                        onToggleLanguage = toggleLanguage,
                        onToggleTheme = toggleTheme,
                        onSetThemeMode = setThemeMode
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedDiyarShell(
    currentUserId: String,
    language: AppLanguage,
    themeMode: AppThemeMode,
    isDark: Boolean,
    deepLinkPropertyId: String?,
    onDeepLinkConsumed: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DiyarViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                DiyarViewModel(DiyarRepository(db), currentUserId, app.applicationContext)
            }
        }
    )

    LaunchedEffect(language) {
        viewModel.syncLanguagePreference(language.code)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(deepLinkPropertyId) {
        if (!deepLinkPropertyId.isNullOrBlank()) {
            navController.navigate(DiyarRoutes.detail(deepLinkPropertyId))
            onDeepLinkConsumed()
        }
    }

    // Bottom Navigation Bar: Explore, Saved, Publish, Services, Profile (Alerts moved to Header Bell + Profile!)
    val destinations = listOf(
        NavDestination(DiyarRoutes.DISCOVER, strings.navDiscover, Icons.Filled.Explore, Icons.Outlined.Explore),
        NavDestination(DiyarRoutes.FAVORITES, strings.navFavorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        NavDestination(DiyarRoutes.PUBLISH, strings.navPublish, Icons.Filled.AddHomeWork, Icons.Outlined.AddHomeWork),
        NavDestination(DiyarRoutes.SERVICES, strings.navServices, Icons.Filled.Handyman, Icons.Outlined.Handyman),
        NavDestination(DiyarRoutes.PROFILE, strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
    )

    val showChrome = currentRoute in destinations.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !useRail) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = DiyarRoutes.DISCOVER
                    ) {
                        composable(DiyarRoutes.DISCOVER) {
                            DiscoverScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onToggleTheme = onToggleTheme,
                                onToggleLanguage = onToggleLanguage,
                                onOpenAlerts = { navController.navigate(DiyarRoutes.ALERTS) },
                                onOpenPropertyDetail = { propId ->
                                    navController.navigate(DiyarRoutes.detail(propId))
                                }
                            )
                        }
                        composable(DiyarRoutes.FAVORITES) {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onOpenDetail = { propId ->
                                    navController.navigate(DiyarRoutes.detail(propId))
                                }
                            )
                        }
                        composable(DiyarRoutes.PUBLISH) {
                            PublishScreen(
                                viewModel = viewModel,
                                onPublished = { propId ->
                                    navController.navigate(DiyarRoutes.detail(propId))
                                }
                            )
                        }
                        composable(DiyarRoutes.SERVICES) {
                            ServicesScreen(viewModel = viewModel)
                        }
                        composable(DiyarRoutes.PROFILE) {
                            ProfileScreen(
                                viewModel = viewModel,
                                themeMode = themeMode,
                                onSetThemeMode = onSetThemeMode,
                                onToggleLanguage = onToggleLanguage,
                                onOpenAlerts = { navController.navigate(DiyarRoutes.ALERTS) },
                                onOpenMyListings = { navigateTopLevel(DiyarRoutes.PUBLISH) },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                        composable(DiyarRoutes.ALERTS) {
                            AlertsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenDetail = { propId ->
                                    navController.navigate(DiyarRoutes.detail(propId))
                                }
                            )
                        }
                        composable(
                            route = DiyarRoutes.DETAIL,
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
                        ) { entry ->
                            val propId = entry.arguments?.getString("propertyId") ?: ""
                            PropertyDetailScreen(
                                propertyId = propId,
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { navController.popBackStack() },
                                onOpenVirtualTour = { id ->
                                    navController.navigate(DiyarRoutes.tour(id))
                                },
                                onOpenServices = { navigateTopLevel(DiyarRoutes.SERVICES) }
                            )
                        }
                        composable(
                            route = DiyarRoutes.TOUR_360,
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
                        ) { entry ->
                            val propId = entry.arguments?.getString("propertyId") ?: ""
                            VirtualTourScreen(
                                propertyId = propId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
