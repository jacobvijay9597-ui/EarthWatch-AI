package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.core.UiState
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.local.db.PredictionHistoryEntity
import com.example.data.model.AiRiskBriefing
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.HistoricalLandslideRecord
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.LocationPermissionState
import com.example.data.model.MlBackendHealthStatus
import com.example.data.model.MlLandslidePredictionResult
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RiskLevel
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.RouteEvaluation
import com.example.data.model.UserLocationSnapshot
import com.example.data.model.UserProfile
import com.example.data.repository.AiLandslideRepository
import com.example.data.repository.FirestoreSafetyRepository
import com.example.data.repository.LandslidePredictionRepository
import com.example.data.repository.LocalSqlCacheRepository
import com.example.data.repository.RiskEngine
import com.example.data.repository.WeatherRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val VM_TAG = "EarthWatchVM"
private const val STOP_TIMEOUT_MS = 5000L

class EarthWatchViewModel(
    private val weatherRepository: WeatherRepository,
    private val aiRepository: AiLandslideRepository,
    private val firestoreRepository: FirestoreSafetyRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String,
    private val mlPredictionRepository: LandslidePredictionRepository = LandslidePredictionRepository(),
    private val localSqlRepository: LocalSqlCacheRepository? = null
) : ViewModel() {

    val isAdminUser: Boolean =
        currentUserEmail.equals("jacobvijay9597@gmail.com", ignoreCase = true)

    private val _weatherState =
        MutableStateFlow<UiState<List<LiveWeatherTelemetry>>>(UiState.Loading)
    val weatherState: StateFlow<UiState<List<LiveWeatherTelemetry>>> = _weatherState.asStateFlow()

    private val _lastWeatherSyncLabel = MutableStateFlow("Not synced yet")
    val lastWeatherSyncLabel: StateFlow<String> = _lastWeatherSyncLabel.asStateFlow()

    val riskEstimates: StateFlow<List<ExperimentalRiskEstimate>> = _weatherState
        .map { state ->
            when (state) {
                is UiState.Success -> RiskEngine.estimateAllCorridors(state.data)
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        firestoreRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catchFirestoreError("Failed to load alert settings")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicReportsState: StateFlow<UiState<List<CommunityReport>>> =
        firestoreRepository.observePublicReports()
            .map<List<CommunityReport>, UiState<List<CommunityReport>>> { UiState.Success(it) }
            .catchFirestoreError("Failed to load community reports")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val adminReportsState: StateFlow<UiState<List<CommunityReport>>> =
        if (isAdminUser) {
            firestoreRepository.observeAllReportsForAdmin()
                .map<List<CommunityReport>, UiState<List<CommunityReport>>> { UiState.Success(it) }
                .catchFirestoreError("Failed to load admin report queue")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        } else {
            publicReportsState
        }

    val advisoriesState: StateFlow<UiState<List<OfficialAdvisory>>> =
        firestoreRepository.observePublishedAdvisories()
            .map<List<OfficialAdvisory>, UiState<List<OfficialAdvisory>>> { UiState.Success(it) }
            .catchFirestoreError("Failed to load advisories")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val corridorStatusesState: StateFlow<UiState<List<RoadCorridorStatus>>> =
        firestoreRepository.observeActiveCorridors()
            .map<List<RoadCorridorStatus>, UiState<List<RoadCorridorStatus>>> { UiState.Success(it) }
            .catchFirestoreError("Failed to load corridor statuses")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // Route Checker selection state (Modules 2 & 6) - defaults to Coimbatore -> Ooty
    private val _originId = MutableStateFlow("coimbatore")
    val originId: StateFlow<String> = _originId.asStateFlow()

    private val _destinationId = MutableStateFlow("ooty")
    val destinationId: StateFlow<String> = _destinationId.asStateFlow()

    private val _travelDateMillis = MutableStateFlow(System.currentTimeMillis())
    val travelDateMillis: StateFlow<Long> = _travelDateMillis.asStateFlow()

    val routeEvaluation: StateFlow<RouteEvaluation> = combine(
        _originId,
        _destinationId,
        _weatherState,
        publicReportsState,
        advisoriesState
    ) { fromId, toId, weatherUi, reportsUi, advisoriesUi ->
        buildRouteEvaluation(
            fromId = fromId,
            toId = toId,
            weatherList = (weatherUi as? UiState.Success)?.data.orEmpty(),
            reports = (reportsUi as? UiState.Success)?.data.orEmpty(),
            advisories = (advisoriesUi as? UiState.Success)?.data.orEmpty(),
            corridorStatuses = (corridorStatusesState.value as? UiState.Success)?.data.orEmpty()
        )
    }.combine(corridorStatusesState) { partial, statusesUi ->
        val statuses = (statusesUi as? UiState.Success)?.data.orEmpty()
        val relevantIds = if (partial.primaryCorridor.id == "coimbatore_ooty") {
            setOf("coimbatore_ooty", "mettupalayam_coonoor", "coonoor_ooty")
        } else {
            setOf(partial.primaryCorridor.id)
        }
        val matchedStatuses = statuses.filter { it.id in relevantIds }
        val primaryStatus = matchedStatuses.firstOrNull { it.id == partial.primaryCorridor.id }
            ?: matchedStatuses.firstOrNull()
        val hasClosedAdvisory = partial.relatedAdvisories.any { it.roadClosed }
        val hasActiveReport = partial.relatedReports.any {
            it.verificationStatus == "UNVERIFIED" || it.verificationStatus == "CONFIRMED"
        }
        val anyClosedStatus = matchedStatuses.any {
            it.status == CorridorOperationalStatus.CLOSED_OFFICIAL.code
        }
        val allRequiredOpenAndVerified = if (partial.primaryCorridor.id == "coimbatore_ooty") {
            val directOpen = primaryStatus?.id == "coimbatore_ooty" &&
                primaryStatus.status == CorridorOperationalStatus.OPEN.code &&
                primaryStatus.verifiedSafety
            val subSegmentsOpen = listOf("mettupalayam_coonoor", "coonoor_ooty").all { segId ->
                val seg = matchedStatuses.firstOrNull { it.id == segId }
                seg?.status == CorridorOperationalStatus.OPEN.code && seg.verifiedSafety
            }
            (directOpen || subSegmentsOpen) && !anyClosedStatus && !hasClosedAdvisory && !hasActiveReport
        } else {
            primaryStatus?.status == CorridorOperationalStatus.OPEN.code &&
                primaryStatus.verifiedSafety &&
                !hasClosedAdvisory &&
                !hasActiveReport
        }

        val safetyNotice = when {
            anyClosedStatus || hasClosedAdvisory ->
                "WARNING: Official road closure or restriction active on this route. Do not travel this corridor."
            hasActiveReport ->
                "CAUTION: Active community incident or hazard report(s) on this route. Road safety cannot be confirmed; inspect official advisories or alternates."
            allRequiredOpenAndVerified ->
                "Verified Open Status Present (Admin confirmed). Continue monitoring live Open-Meteo rainfall before hill ascent."
            else ->
                "SAFETY UNCONFIRMED: No reliable official road-status confirmation is available for this corridor. EarthWatch AI never marks a road as safe without verified road-status data."
        }
        partial.copy(
            corridorStatus = primaryStatus,
            safetyConfirmed = allRequiredOpenAndVerified,
            safetyNotice = safetyNotice
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        buildRouteEvaluation("coimbatore", "ooty", emptyList(), emptyList(), emptyList(), emptyList())
    )

    // Module 3: Random Forest ML & AI Landslide Prediction
    private val _selectedAiCorridorId = MutableStateFlow("coimbatore_ooty")
    val selectedAiCorridorId: StateFlow<String> = _selectedAiCorridorId.asStateFlow()

    private val _mlBackendUrl = MutableStateFlow(BuildConfig.ML_BACKEND_DEFAULT_URL)
    val mlBackendUrl: StateFlow<String> = _mlBackendUrl.asStateFlow()

    private val _mlBackendHealthState = MutableStateFlow<MlBackendHealthStatus?>(null)
    val mlBackendHealthState: StateFlow<MlBackendHealthStatus?> = _mlBackendHealthState.asStateFlow()

    private val _isCheckingMlBackend = MutableStateFlow(false)
    val isCheckingMlBackend: StateFlow<Boolean> = _isCheckingMlBackend.asStateFlow()

    private val _selectedDemoScenarioId = MutableStateFlow<String?>(null)
    val selectedDemoScenarioId: StateFlow<String?> = _selectedDemoScenarioId.asStateFlow()

    private val _mlPredictionState =
        MutableStateFlow<UiState<MlLandslidePredictionResult>>(UiState.Loading)
    val mlPredictionState: StateFlow<UiState<MlLandslidePredictionResult>> =
        _mlPredictionState.asStateFlow()

    private val _aiBriefingState = MutableStateFlow<UiState<AiRiskBriefing?>?>(null)
    val aiBriefingState: StateFlow<UiState<AiRiskBriefing?>?> = _aiBriefingState.asStateFlow()

    val recentPredictionLogs: StateFlow<List<PredictionHistoryEntity>> =
        (localSqlRepository?.observeRecentPredictionLogs(10) ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sqlHistoricalRecords: StateFlow<List<HistoricalLandslideRecord>> =
        _selectedAiCorridorId.flatMapLatest { corridorId ->
            localSqlRepository?.observeHistoricalRecordsByCorridor(corridorId)
                ?: flowOf(
                    NilgirisReferenceCatalog.simulatedHistoricalRecords.filter {
                        it.corridorId == corridorId
                    }
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Real-Time Fine Location & Positional Situational Awareness
    private val _locationPermissionState = MutableStateFlow(LocationPermissionState.UNKNOWN)
    val locationPermissionState: StateFlow<LocationPermissionState> = _locationPermissionState.asStateFlow()

    private val _hasRequestedLocationPermission = MutableStateFlow(false)
    val hasRequestedLocationPermission: StateFlow<Boolean> = _hasRequestedLocationPermission.asStateFlow()

    private val _isLiveTrackingEnabled = MutableStateFlow(true)
    val isLiveTrackingEnabled: StateFlow<Boolean> = _isLiveTrackingEnabled.asStateFlow()

    private val _userLocation = MutableStateFlow<UserLocationSnapshot?>(null)
    val userLocation: StateFlow<UserLocationSnapshot?> = _userLocation.asStateFlow()

    // AI Traveller Safety Assistant Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Welcome to EarthWatch AI Assistant. Ask about Nilgiris ghat weather, experimental landslide factors, or alternate hill corridors between Mettupalayam, Coonoor, Ooty, Kotagiri, and Gudalur."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isSubmittingAction = MutableStateFlow(false)
    val isSubmittingAction: StateFlow<Boolean> = _isSubmittingAction.asStateFlow()

    private var chatJob: Job? = null
    private var mlPredictionJob: Job? = null

    init {
        if (localSqlRepository != null) {
            viewModelScope.launch {
                runCatching { localSqlRepository.ensureHistoricalCatalogSeeded() }
            }
        }
        refreshWeather()
        checkMlBackendHealth()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading
            val result = weatherRepository.fetchAllNilgirisWeather()
            result.fold(
                onSuccess = { list ->
                    _weatherState.value = UiState.Success(list)
                    val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US).format(Date())
                    _lastWeatherSyncLabel.value = "$stamp IST"
                    runCatching { localSqlRepository?.cacheWeatherTelemetry(list) }
                    runMlLandslidePrediction()
                },
                onFailure = { err ->
                    Log.e(VM_TAG, "Live weather fetch failed", err)
                    val cached = runCatching {
                        localSqlRepository?.loadCachedWeatherFallback().orEmpty()
                    }.getOrDefault(emptyList())
                    if (cached.isNotEmpty()) {
                        _weatherState.value = UiState.Success(cached)
                        _lastWeatherSyncLabel.value = cached.first().fetchedAtLabel
                        _actionMessage.value = "Offline: loaded last synced Open-Meteo telemetry from local SQLite cache."
                    } else {
                        _weatherState.value = UiState.Error(
                            err.localizedMessage ?: "Unable to fetch live weather from Open-Meteo. Check internet connectivity."
                        )
                    }
                    runMlLandslidePrediction()
                }
            )
        }
    }

    fun setOrigin(locationId: String) {
        _originId.value = locationId
    }

    fun setDestination(locationId: String) {
        _destinationId.value = locationId
    }

    fun swapOriginDestination() {
        val prevOrigin = _originId.value
        _originId.value = _destinationId.value
        _destinationId.value = prevOrigin
    }

    fun setTravelDateMillis(millis: Long) {
        _travelDateMillis.value = millis
    }

    fun selectAiCorridor(corridorId: String) {
        _selectedAiCorridorId.value = corridorId
        _aiBriefingState.value = null
        runMlLandslidePrediction()
    }

    fun updateMlBackendUrl(url: String) {
        _mlBackendUrl.value = url
    }

    fun selectDemoScenario(scenarioId: String?) {
        _selectedDemoScenarioId.value = scenarioId
        runMlLandslidePrediction()
    }

    fun checkMlBackendHealth() {
        viewModelScope.launch {
            _isCheckingMlBackend.value = true
            val health = mlPredictionRepository.checkBackendHealth(_mlBackendUrl.value)
            _mlBackendHealthState.value = health
            _isCheckingMlBackend.value = false
            runMlLandslidePrediction()
        }
    }

    fun runMlLandslidePrediction() {
        val corridor = NilgirisReferenceCatalog.corridorById(_selectedAiCorridorId.value)
        val demoPreset = _selectedDemoScenarioId.value?.let { id ->
            NilgirisReferenceCatalog.demoScenarios.firstOrNull { it.id == id }
        }
        val weatherUi = _weatherState.value
        if (weatherUi is UiState.Loading && demoPreset == null) {
            _mlPredictionState.value = UiState.Loading
            return
        }
        val weatherList = (weatherUi as? UiState.Success)?.data.orEmpty()
        val backendUrl = _mlBackendUrl.value

        mlPredictionJob?.cancel()
        mlPredictionJob = viewModelScope.launch {
            _mlPredictionState.value = UiState.Loading
            val result = mlPredictionRepository.evaluateCorridorRisk(
                corridor = corridor,
                weatherList = weatherList,
                backendBaseUrl = backendUrl,
                demoScenarioOverride = demoPreset
            )
            _mlPredictionState.value = UiState.Success(result)
            runCatching { localSqlRepository?.recordMlPrediction(result) }
        }
    }

    fun clearPredictionSqlLogs() {
        viewModelScope.launch {
            runCatching { localSqlRepository?.clearPredictionLogs() }
            _actionMessage.value = "Cleared local SQLite prediction history."
        }
    }

    fun runGeminiExperimentalAnalysis() {
        val corridorId = _selectedAiCorridorId.value
        val corridor = NilgirisReferenceCatalog.corridorById(corridorId)
        val weatherList = (_weatherState.value as? UiState.Success)?.data.orEmpty()
        val estimate = riskEstimates.value.firstOrNull { it.corridorId == corridorId }
        if (weatherList.isEmpty() || estimate == null) {
            _aiBriefingState.value = UiState.Error(
                "Live weather telemetry is required before running experimental AI risk analysis. Please sync weather first."
            )
            return
        }

        val relevantWeather = weatherList.filter {
            it.locationId == corridor.fromLocationId || it.locationId == corridor.toLocationId
        }

        viewModelScope.launch {
            _aiBriefingState.value = UiState.Loading
            val result = aiRepository.generateExperimentalBriefing(corridor, estimate, relevantWeather)
            result.fold(
                onSuccess = { briefing ->
                    _aiBriefingState.value = UiState.Success(briefing)
                },
                onFailure = { err ->
                    _aiBriefingState.value = UiState.Error(
                        err.localizedMessage ?: "Gemini experimental analysis failed."
                    )
                }
            )
        }
    }

    fun sendAssistantQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _isChatStreaming.value) return

        val weatherList = (_weatherState.value as? UiState.Success)?.data.orEmpty()
        val telemetryContext = if (weatherList.isEmpty()) {
            "Live Open-Meteo telemetry unavailable right now."
        } else {
            weatherList.joinToString("\n") { w ->
                val soilText = w.soilMoistureM3M3?.let { "${it}m3/m3" } ?: "unavailable"
                "${w.locationName}: ${w.temperatureC}°C, rainNow=${w.rainCurrentMm}mm/h, rain24h=${w.rain24hMm}mm, rain72h=${w.rain72hMm}mm, soil=$soilText, condition=${w.weatherLabel}"
            }
        }

        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), isUser = true, text = trimmed)
        val botId = UUID.randomUUID().toString()
        val botPlaceholder = ChatMessage(id = botId, isUser = false, text = "", isStreaming = true)

        _chatMessages.update { it + userMsg + botPlaceholder }
        _chatError.value = null
        _isChatStreaming.value = true

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                aiRepository.streamTravellerAssistant(trimmed, telemetryContext).collect { partial ->
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == botId) msg.copy(text = partial, isStreaming = true) else msg
                        }
                    }
                }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == botId) msg.copy(isStreaming = false) else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Assistant stream error", e)
                _chatError.value = e.localizedMessage ?: "AI Assistant error."
                _chatMessages.update { list -> list.filterNot { it.id == botId && it.text.isBlank() } }
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    fun saveAlertSubscriptions(
        subscribedCorridors: List<String>,
        alertsEnabled: Boolean,
        riskThreshold: String
    ) {
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.saveUserAlertSettings(
                displayName = currentUserName,
                email = currentUserEmail,
                subscribedCorridors = subscribedCorridors,
                alertsEnabled = alertsEnabled,
                riskThreshold = riskThreshold
            )
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Alert subscriptions saved to Firestore." },
                onFailure = { it.localizedMessage ?: "Failed to save alert settings." }
            )
        }
    }

    fun submitCommunityReport(
        corridorId: String,
        locationLabel: String,
        incidentType: String,
        severity: String,
        description: String,
        observedTimeText: String
    ) {
        val corridor = NilgirisReferenceCatalog.corridorById(corridorId)
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.submitCommunityReport(
                reporterName = currentUserName,
                corridorId = corridor.id,
                corridorName = corridor.name,
                locationLabel = locationLabel,
                incidentType = incidentType,
                severity = severity,
                description = description,
                observedTimeText = observedTimeText
            )
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Report submitted to Firestore (labeled UNVERIFIED until admin review)." },
                onFailure = { it.localizedMessage ?: "Failed to submit community report." }
            )
        }
    }

    fun adminVerifyOrUpdateReport(
        reportId: String,
        newStatus: String,
        visibility: String,
        adminNotes: String
    ) {
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.adminUpdateReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                visibility = visibility,
                adminNotes = adminNotes
            )
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Report status updated to $newStatus." },
                onFailure = { it.localizedMessage ?: "Failed to update report." }
            )
        }
    }

    fun adminUpdateCorridorOperationalStatus(
        corridorId: String,
        status: String,
        statusNote: String,
        verifiedSafety: Boolean,
        simulatedDemo: Boolean
    ) {
        val corridor = NilgirisReferenceCatalog.corridorById(corridorId)
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.adminSetCorridorStatus(
                corridorId = corridor.id,
                corridorName = corridor.name,
                status = status,
                statusNote = statusNote,
                verifiedSafety = verifiedSafety,
                simulatedDemo = simulatedDemo
            )
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Updated ${corridor.name} status to $status." },
                onFailure = { it.localizedMessage ?: "Failed to update corridor status." }
            )
        }
    }

    fun adminCreateAdvisory(
        title: String,
        corridorId: String,
        severity: String,
        sourceType: String,
        sourceLabel: String,
        summary: String,
        roadClosed: Boolean,
        simulatedDemo: Boolean
    ) {
        val corridor = NilgirisReferenceCatalog.corridorById(corridorId)
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.adminPublishAdvisory(
                title = title,
                corridorId = corridor.id,
                corridorName = corridor.name,
                severity = severity,
                sourceType = sourceType,
                sourceLabel = sourceLabel,
                summary = summary,
                roadClosed = roadClosed,
                simulatedDemo = simulatedDemo
            )
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Published advisory for ${corridor.name}." },
                onFailure = { it.localizedMessage ?: "Failed to publish advisory." }
            )
        }
    }

    fun adminSeedDemoRecords() {
        viewModelScope.launch {
            _isSubmittingAction.value = true
            val result = firestoreRepository.adminSeedSimulatedHackathonData()
            _isSubmittingAction.value = false
            _actionMessage.value = result.fold(
                onSuccess = { "Seeded clearly labeled [SIMULATED DEMO] advisories & corridor statuses." },
                onFailure = { it.localizedMessage ?: "Failed to seed demo data." }
            )
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun updateLocationPermissionState(state: LocationPermissionState, markRequested: Boolean = false) {
        if (markRequested) {
            _hasRequestedLocationPermission.value = true
        }
        _locationPermissionState.value = state
        if (state != LocationPermissionState.FINE_GRANTED && state != LocationPermissionState.APPROXIMATE_ONLY) {
            _userLocation.value = null
        }
    }

    fun toggleLiveLocationTracking() {
        val next = !_isLiveTrackingEnabled.value
        _isLiveTrackingEnabled.value = next
        if (!next) {
            _userLocation.value = null
        }
    }

    fun onUserLocationUpdated(snapshot: UserLocationSnapshot) {
        _userLocation.value = snapshot
    }

    private fun buildRouteEvaluation(
        fromId: String,
        toId: String,
        weatherList: List<LiveWeatherTelemetry>,
        reports: List<CommunityReport>,
        advisories: List<OfficialAdvisory>,
        corridorStatuses: List<RoadCorridorStatus>
    ): RouteEvaluation {
        val (primary, alts) = NilgirisReferenceCatalog.findCorridorsBetween(fromId, toId)
        val weatherMap = weatherList.associateBy { it.locationId }
        val estimate = RiskEngine.estimateCorridorRisk(primary, weatherMap)
        val stopIds = if (primary.id == "coimbatore_ooty") {
            listOf("coimbatore", "mettupalayam", "coonoor", "ooty")
        } else {
            listOf(primary.fromLocationId, primary.toLocationId, fromId, toId)
        }
        val stopsWeather = stopIds.mapNotNull { weatherMap[it] }.distinctBy { it.locationId }

        val relevantCorridorIds = if (primary.id == "coimbatore_ooty") {
            setOf("coimbatore_ooty", "mettupalayam_coonoor", "coonoor_ooty")
        } else {
            setOf(primary.id)
        }
        val relatedReports = reports.filter {
            it.corridorId in relevantCorridorIds && it.verificationStatus != "DISMISSED" && it.verificationStatus != "RESOLVED"
        }
        val relatedAdvisories = advisories.filter { it.corridorId in relevantCorridorIds }
        val status = corridorStatuses.firstOrNull { it.id == primary.id }

        return RouteEvaluation(
            primaryCorridor = primary,
            alternateCorridors = alts,
            riskEstimate = estimate,
            weatherAtStops = stopsWeather,
            relatedReports = relatedReports,
            relatedAdvisories = relatedAdvisories,
            corridorStatus = status,
            safetyConfirmed = false,
            safetyNotice = "SAFETY UNCONFIRMED: Verifying reliable road-status records..."
        )
    }

    private fun <T> kotlinx.coroutines.flow.Flow<UiState<T>>.catchFirestoreError(
        fallbackMessage: String
    ): kotlinx.coroutines.flow.Flow<UiState<T>> {
        return this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(VM_TAG, fallbackMessage, rootError)
            emit(UiState.Error(rootError.message ?: fallbackMessage))
        }
    }
}
