package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CommunityReport
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSafetyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must sign in with Google before accessing Firestore.")

    private fun <T> Flow<T>.wrapFirestoreFlowErrors(
        uid: String,
        op: OperationType,
        path: String
    ): Flow<T> = this.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, op, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Profile access restricted to authenticated owner." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreFlowErrors(uid, OperationType.GET, path)
        )
    }

    suspend fun saveUserAlertSettings(
        displayName: String,
        email: String,
        subscribedCorridors: List<String>,
        alertsEnabled: Boolean,
        riskThreshold: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val safeName = displayName.trim().ifBlank { "Nilgiris Traveller" }.take(80)
            val safeEmail = email.trim().ifBlank { "traveller@earthwatch.local" }.take(120)
            val boundedCorridors = subscribedCorridors.distinct().take(10)
            val safeThreshold = if (riskThreshold in listOf("LOW", "MEDIUM", "HIGH")) riskThreshold else "MEDIUM"

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "subscribedCorridors" to boundedCorridors,
                        "alertsEnabled" to alertsEnabled,
                        "riskThreshold" to safeThreshold,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "subscribedCorridors" to boundedCorridors,
                        "alertsEnabled" to alertsEnabled,
                        "riskThreshold" to safeThreshold,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observePublicReports(): Flow<List<CommunityReport>> = flow {
        val uid = requireUserId()
        val path = "community_reports"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreFlowErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAllReportsForAdmin(): Flow<List<CommunityReport>> = flow {
        val uid = requireUserId()
        val path = "community_reports"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreFlowErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun submitCommunityReport(
        reporterName: String,
        corridorId: String,
        corridorName: String,
        locationLabel: String,
        incidentType: String,
        severity: String,
        description: String,
        observedTimeText: String
    ): Result<Unit> {
        var docPath = "community_reports"
        return try {
            val uid = requireUserId()
            val cleanLocation = locationLabel.trim()
            val cleanDescription = description.trim()
            val cleanTime = observedTimeText.trim()
            require(cleanLocation.length in 2..120) { "Location must be 2 to 120 characters." }
            require(cleanDescription.length in 5..500) { "Description must be 5 to 500 characters." }
            require(cleanTime.length in 2..60) { "Observed time must be 2 to 60 characters." }

            val docRef = db.collection("community_reports").document()
            docPath = docRef.path
            val payload = mapOf(
                "reporterUid" to uid,
                "reporterName" to reporterName.trim().ifBlank { "Traveller" }.take(60),
                "corridorId" to corridorId.take(64),
                "corridorName" to corridorName.take(100),
                "locationLabel" to cleanLocation,
                "incidentType" to incidentType,
                "severity" to severity,
                "description" to cleanDescription,
                "observedTimeText" to cleanTime,
                "verificationStatus" to "UNVERIFIED",
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateReportStatus(
        reportId: String,
        newStatus: String,
        visibility: String,
        adminNotes: String
    ): Result<Unit> {
        val docPath = "community_reports/$reportId"
        return try {
            requireUserId()
            require(reportId.isNotBlank()) { "Report ID is required." }
            db.collection("community_reports").document(reportId)
                .update(
                    mapOf(
                        "verificationStatus" to newStatus,
                        "visibility" to visibility,
                        "adminNotes" to adminNotes.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observePublishedAdvisories(): Flow<List<OfficialAdvisory>> = flow {
        val uid = requireUserId()
        val path = "official_advisories"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OfficialAdvisory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreFlowErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeActiveCorridors(): Flow<List<RoadCorridorStatus>> = flow {
        val uid = requireUserId()
        val path = "road_corridors"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoadCorridorStatus::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .wrapFirestoreFlowErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun adminPublishAdvisory(
        title: String,
        corridorId: String,
        corridorName: String,
        severity: String,
        sourceType: String,
        sourceLabel: String,
        summary: String,
        roadClosed: Boolean,
        simulatedDemo: Boolean
    ): Result<Unit> {
        var docPath = "official_advisories"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanSummary = summary.trim()
            require(cleanTitle.length in 3..120) { "Title must be 3–120 characters." }
            require(cleanSummary.length in 5..500) { "Summary must be 5–500 characters." }

            val docRef = db.collection("official_advisories").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "corridorId" to corridorId,
                    "corridorName" to corridorName,
                    "severity" to severity,
                    "sourceType" to sourceType,
                    "sourceLabel" to sourceLabel.trim().take(100),
                    "summary" to cleanSummary,
                    "roadClosed" to roadClosed,
                    "published" to true,
                    "simulatedDemo" to simulatedDemo,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSetCorridorStatus(
        corridorId: String,
        corridorName: String,
        status: String,
        statusNote: String,
        verifiedSafety: Boolean,
        simulatedDemo: Boolean
    ): Result<Unit> {
        val docPath = "road_corridors/$corridorId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("road_corridors").document(corridorId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "corridorName" to corridorName.take(100),
                        "status" to status,
                        "statusNote" to statusNote.trim().ifBlank { "Status updated by admin." }.take(300),
                        "verifiedSafety" to verifiedSafety,
                        "active" to true,
                        "simulatedDemo" to simulatedDemo,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "corridorName" to corridorName.take(100),
                        "status" to status,
                        "statusNote" to statusNote.trim().ifBlank { "Initialized by admin." }.take(300),
                        "verifiedSafety" to verifiedSafety,
                        "active" to true,
                        "simulatedDemo" to simulatedDemo,
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminSeedSimulatedHackathonData(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val batch = db.batch()

            val demoCorridors = listOf(
                Triple(
                    "mettupalayam_coonoor",
                    "Mettupalayam – Coonoor Ghat",
                    Triple("CAUTION", "Simulated Demo: Monsoon debris watch near Burliar; single-lane caution.", false)
                ),
                Triple(
                    "mettupalayam_kotagiri_ooty",
                    "Mettupalayam – Kotagiri – Ooty",
                    Triple("OPEN", "Simulated Demo: Primary alternate ghat route open; verify live rain before departure.", true)
                ),
                Triple(
                    "kalhatti_ghat",
                    "Ooty – Kalhatti – Masinagudi",
                    Triple("CLOSED_OFFICIAL", "Simulated Demo: Downhill descent restricted for non-local vehicles during wet weather.", false)
                )
            )

            for ((id, name, meta) in demoCorridors) {
                val ref = db.collection("road_corridors").document(id)
                val existing = ref.get().await()
                if (existing.exists()) {
                    batch.update(
                        ref,
                        mapOf(
                            "corridorName" to name,
                            "status" to meta.first,
                            "statusNote" to meta.second,
                            "verifiedSafety" to meta.third,
                            "active" to true,
                            "simulatedDemo" to true,
                            "updatedByUid" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        ref,
                        mapOf(
                            "corridorName" to name,
                            "status" to meta.first,
                            "statusNote" to meta.second,
                            "verifiedSafety" to meta.third,
                            "active" to true,
                            "simulatedDemo" to true,
                            "updatedByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val demoAdvisories = listOf(
                Pair(
                    "demo_adv_burliar",
                    mapOf(
                        "title" to "NH-181 Burliar Slope Precaution (Simulated Demo)",
                        "corridorId" to "mettupalayam_coonoor",
                        "corridorName" to "Mettupalayam – Coonoor Ghat",
                        "severity" to "HIGH",
                        "sourceType" to "OFFICIAL_ADVISORY",
                        "sourceLabel" to "Simulated District Control Demo",
                        "summary" to "Sample hackathon demonstration advisory: Expect intermittent boulder clearance between Kallar and Marappalam after heavy showers.",
                        "roadClosed" to false,
                        "published" to true,
                        "simulatedDemo" to true
                    )
                ),
                Pair(
                    "demo_adv_kalhatti",
                    mapOf(
                        "title" to "Kalhatti 36-Hairpin Descent Restriction (Simulated Demo)",
                        "corridorId" to "kalhatti_ghat",
                        "corridorName" to "Ooty – Kalhatti – Masinagudi",
                        "severity" to "CRITICAL",
                        "sourceType" to "OFFICIAL_CLOSURE",
                        "sourceLabel" to "Simulated Highway Patrol Demo",
                        "summary" to "Sample hackathon demonstration closure: Steep Sigur Ghat closed to tourist downhill traffic; use Gudalur NH-181 West instead.",
                        "roadClosed" to true,
                        "published" to true,
                        "simulatedDemo" to true
                    )
                )
            )

            for ((advId, fields) in demoAdvisories) {
                val advRef = db.collection("official_advisories").document(advId)
                val existing = advRef.get().await()
                if (existing.exists()) {
                    batch.update(
                        advRef,
                        fields + mapOf("updatedAt" to FieldValue.serverTimestamp())
                    )
                } else {
                    batch.set(
                        advRef,
                        fields + mapOf(
                            "createdByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "simulated_demo_seed")
            Result.failure(e)
        }
    }
}
