package com.example.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.authStateFlow
import com.example.core.signOutUser
import com.example.data.location.UserLocationTracker
import com.example.data.model.LocationPermissionState
import com.example.data.repository.AiLandslideRepository
import com.example.data.repository.FirestoreSafetyRepository
import com.example.data.repository.LandslidePredictionRepository
import com.example.data.repository.LocalSqlCacheRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.screens.AiPredictionScreen
import com.example.ui.screens.AlertsReportsAdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NilgirisRiskMapScreen
import com.example.ui.screens.RouteCheckerScreen
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateLight
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import com.example.viewmodel.EarthWatchViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class EarthWatchDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Routes("routes", "Routes", Icons.Filled.Route, Icons.Outlined.Route),
    RiskMap("risk_map", "Risk Map", Icons.Filled.Map, Icons.Outlined.Map),
    AiRisk("ai_risk", "AI Risk", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    AlertsHub("alerts_hub", "Alerts", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive)
}

@Composable
fun EarthWatchApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedEarthWatchShell(
                currentUserId = user.uid,
                currentUserName = user.displayName ?: "Nilgiris Traveller",
                currentUserEmail = user.email ?: ""
            )
        }
    }
}

@Composable
private fun AuthenticatedEarthWatchShell(
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: EarthWatchViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                EarthWatchViewModel(
                    weatherRepository = WeatherRepository(),
                    aiRepository = AiLandslideRepository(),
                    firestoreRepository = FirestoreSafetyRepository(db),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail,
                    mlPredictionRepository = LandslidePredictionRepository(),
                    localSqlRepository = LocalSqlCacheRepository(app)
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EarthWatchDestination.Home.route

    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val lastSyncLabel by viewModel.lastWeatherSyncLabel.collectAsStateWithLifecycle()
    val riskEstimates by viewModel.riskEstimates.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val publicReportsState by viewModel.publicReportsState.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val advisoriesState by viewModel.advisoriesState.collectAsStateWithLifecycle()
    val corridorStatusesState by viewModel.corridorStatusesState.collectAsStateWithLifecycle()
    val originId by viewModel.originId.collectAsStateWithLifecycle()
    val destinationId by viewModel.destinationId.collectAsStateWithLifecycle()
    val travelDateMillis by viewModel.travelDateMillis.collectAsStateWithLifecycle()
    val routeEvaluation by viewModel.routeEvaluation.collectAsStateWithLifecycle()
    val selectedAiCorridorId by viewModel.selectedAiCorridorId.collectAsStateWithLifecycle()
    val mlBackendUrl by viewModel.mlBackendUrl.collectAsStateWithLifecycle()
    val mlBackendHealthState by viewModel.mlBackendHealthState.collectAsStateWithLifecycle()
    val isCheckingMlBackend by viewModel.isCheckingMlBackend.collectAsStateWithLifecycle()
    val selectedDemoScenarioId by viewModel.selectedDemoScenarioId.collectAsStateWithLifecycle()
    val mlPredictionState by viewModel.mlPredictionState.collectAsStateWithLifecycle()
    val aiBriefingState by viewModel.aiBriefingState.collectAsStateWithLifecycle()
    val recentPredictionLogs by viewModel.recentPredictionLogs.collectAsStateWithLifecycle()
    val sqlHistoricalRecords by viewModel.sqlHistoricalRecords.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isSubmittingAction by viewModel.isSubmittingAction.collectAsStateWithLifecycle()
    val locationPermissionState by viewModel.locationPermissionState.collectAsStateWithLifecycle()
    val hasRequestedLocationPermission by viewModel.hasRequestedLocationPermission.collectAsStateWithLifecycle()
    val isLiveTrackingEnabled by viewModel.isLiveTrackingEnabled.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val updated = UserLocationTracker.evaluateFromGrantResults(context, results)
        viewModel.updateLocationPermissionState(updated, markRequested = true)
    }

    val requestFineLocationPermission: () -> Unit = {
        locationPermissionLauncher.launch(UserLocationTracker.REQUIRED_LOCATION_PERMISSIONS)
    }
    val openLocationSettings: () -> Unit = {
        runCatching {
            context.startActivity(UserLocationTracker.buildAppSettingsIntent(context))
        }
    }

    DisposableEffect(lifecycleOwner, hasRequestedLocationPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val evaluated = UserLocationTracker.evaluateLocationPermission(
                    context = context,
                    hasRequestedBefore = hasRequestedLocationPermission
                )
                viewModel.updateLocationPermissionState(evaluated)
            }
        }
        viewModel.updateLocationPermissionState(
            UserLocationTracker.evaluateLocationPermission(
                context = context,
                hasRequestedBefore = hasRequestedLocationPermission
            )
        )
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner, locationPermissionState, isLiveTrackingEnabled) {
        val canTrack = locationPermissionState == LocationPermissionState.FINE_GRANTED ||
            locationPermissionState == LocationPermissionState.APPROXIMATE_ONLY
        if (canTrack && isLiveTrackingEnabled) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserLocationTracker.observeRealTimeLocation(
                    context = context,
                    finePrecision = locationPermissionState == LocationPermissionState.FINE_GRANTED
                ).collect { snapshot ->
                    viewModel.onUserLocationUpdated(snapshot)
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = NavyDeepest,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = NavySurface,
                        tonalElevation = 4.dp
                    ) {
                        EarthWatchDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TealBright,
                                    selectedTextColor = TealBright,
                                    indicatorColor = TealContainer,
                                    unselectedIconColor = SlateLight,
                                    unselectedTextColor = SlateLight
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
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
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = NavyCard,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        EarthWatchDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TealBright,
                                    selectedTextColor = TealBright,
                                    indicatorColor = TealContainer,
                                    unselectedIconColor = SlateLight,
                                    unselectedTextColor = SlateLight
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = EarthWatchDestination.Home.route
                    ) {
                        composable(EarthWatchDestination.Home.route) {
                            HomeDashboardScreen(
                                weatherState = weatherState,
                                lastSyncLabel = lastSyncLabel,
                                riskEstimates = riskEstimates,
                                advisoriesState = advisoriesState,
                                reportsState = publicReportsState,
                                onRefreshWeather = viewModel::refreshWeather,
                                onNavigateRouteChecker = { navigateToTab(EarthWatchDestination.Routes.route) },
                                onNavigateAiPrediction = { navigateToTab(EarthWatchDestination.AiRisk.route) },
                                onNavigateAlerts = { navigateToTab(EarthWatchDestination.AlertsHub.route) },
                                onSignOut = {
                                    signOutUser(CredentialManager.create(context), scope)
                                }
                            )
                        }
                        composable(EarthWatchDestination.Routes.route) {
                            RouteCheckerScreen(
                                originId = originId,
                                destinationId = destinationId,
                                travelDateMillis = travelDateMillis,
                                routeEvaluation = routeEvaluation,
                                allEstimates = riskEstimates,
                                weatherState = weatherState,
                                advisoriesState = advisoriesState,
                                reportsState = publicReportsState,
                                corridorStatusesState = corridorStatusesState,
                                onOriginSelected = viewModel::setOrigin,
                                onDestinationSelected = viewModel::setDestination,
                                onSwapLocations = viewModel::swapOriginDestination,
                                onDateSelected = viewModel::setTravelDateMillis,
                                userLocation = userLocation,
                                locationPermissionState = locationPermissionState,
                                isLiveTrackingEnabled = isLiveTrackingEnabled,
                                onRequestLocationPermission = requestFineLocationPermission,
                                onOpenLocationSettings = openLocationSettings,
                                onToggleLiveTracking = viewModel::toggleLiveLocationTracking
                            )
                        }
                        composable(EarthWatchDestination.RiskMap.route) {
                            NilgirisRiskMapScreen(
                                riskEstimates = riskEstimates,
                                weatherState = weatherState,
                                reportsState = publicReportsState,
                                advisoriesState = advisoriesState,
                                corridorStatusesState = corridorStatusesState,
                                userLocation = userLocation,
                                locationPermissionState = locationPermissionState,
                                isLiveTrackingEnabled = isLiveTrackingEnabled,
                                onRequestLocationPermission = requestFineLocationPermission,
                                onOpenLocationSettings = openLocationSettings,
                                onToggleLiveTracking = viewModel::toggleLiveLocationTracking
                            )
                        }
                        composable(EarthWatchDestination.AiRisk.route) {
                            AiPredictionScreen(
                                selectedCorridorId = selectedAiCorridorId,
                                riskEstimates = riskEstimates,
                                mlBackendUrl = mlBackendUrl,
                                mlBackendHealth = mlBackendHealthState,
                                isCheckingMlBackend = isCheckingMlBackend,
                                selectedDemoScenarioId = selectedDemoScenarioId,
                                mlPredictionState = mlPredictionState,
                                aiBriefingState = aiBriefingState,
                                recentPredictionLogs = recentPredictionLogs,
                                sqlHistoricalRecords = sqlHistoricalRecords,
                                chatMessages = chatMessages,
                                isChatStreaming = isChatStreaming,
                                chatError = chatError,
                                onSelectCorridor = viewModel::selectAiCorridor,
                                onBackendUrlChanged = viewModel::updateMlBackendUrl,
                                onCheckBackendHealth = viewModel::checkMlBackendHealth,
                                onSelectDemoScenario = viewModel::selectDemoScenario,
                                onRunMlPrediction = viewModel::runMlLandslidePrediction,
                                onRunAiBriefing = viewModel::runGeminiExperimentalAnalysis,
                                onClearSqlLogs = viewModel::clearPredictionSqlLogs,
                                onSendChatQuestion = viewModel::sendAssistantQuestion
                            )
                        }
                        composable(EarthWatchDestination.AlertsHub.route) {
                            AlertsReportsAdminScreen(
                                isAdminUser = viewModel.isAdminUser,
                                lastWeatherSyncLabel = lastSyncLabel,
                                userProfileState = userProfileState,
                                riskEstimates = riskEstimates,
                                advisoriesState = advisoriesState,
                                publicReportsState = publicReportsState,
                                adminReportsState = adminReportsState,
                                corridorStatusesState = corridorStatusesState,
                                isSubmittingAction = isSubmittingAction,
                                onSaveSubscriptions = viewModel::saveAlertSubscriptions,
                                onSubmitReport = viewModel::submitCommunityReport,
                                onAdminUpdateReport = viewModel::adminVerifyOrUpdateReport,
                                onAdminUpdateCorridor = viewModel::adminUpdateCorridorOperationalStatus,
                                onAdminCreateAdvisory = viewModel::adminCreateAdvisory,
                                onAdminSeedDemo = viewModel::adminSeedDemoRecords
                            )
                        }
                    }
                }
            }
        }
    }
}
