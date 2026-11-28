package com.example.data.remote

import android.content.Context
import androidx.room.withTransaction
import com.example.R
import com.example.data.local.AppNotificationEntity
import com.example.data.local.AuthorizedMobileEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConcordDatabase
import com.example.auth.AdminCredentialCrypto
import com.example.data.local.OpeningStockEntity
import com.example.data.local.PendingStoreTransferEntity
import com.example.data.local.ProductCategoryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.RegisteredDeviceEntity
import com.example.data.local.SecurityAuditLogEntity
import com.example.data.local.StaffAttendanceEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.StaffFeaturePermission
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface WorkspaceConnectionState {
    data object Checking : WorkspaceConnectionState
    data class NeedsSetup(val userEmail: String, val isAdminEmail: Boolean) : WorkspaceConnectionState
    data class Connected(
        val workspaceId: String,
        val companyName: String,
        val inviteCode: String,
        val memberCount: Int,
        val isOwnerOrAdmin: Boolean,
        val lastSyncNote: String? = null
    ) : WorkspaceConnectionState
    data class Error(val message: String) : WorkspaceConnectionState
}

data class CloudLoginMetadata(
    val cloudUserRole: UserRole = UserRole.STAFF,
    val linkedStaffUsername: String = "",
    val linkedStaffRole: UserRole? = null,
    val verifiedInCloud: Boolean = false
)

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val localDb: ConcordDatabase,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, localDb: ConcordDatabase) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localDb
    )

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeSyncJob: Job? = null

    private val _connectionState = MutableStateFlow<WorkspaceConnectionState>(WorkspaceConnectionState.Checking)
    val connectionState: StateFlow<WorkspaceConnectionState> = _connectionState.asStateFlow()

    private val _loginMetadata = MutableStateFlow(CloudLoginMetadata())
    val loginMetadata: StateFlow<CloudLoginMetadata> = _loginMetadata.asStateFlow()

    private val _adminSettings = MutableStateFlow<CloudAdminSettings?>(null)
    val adminSettings: StateFlow<CloudAdminSettings?> = _adminSettings.asStateFlow()

    private val _adminReports = MutableStateFlow<List<CloudAdminReportSnapshot>>(emptyList())
    val adminReports: StateFlow<List<CloudAdminReportSnapshot>> = _adminReports.asStateFlow()

    private val _staffLoginEvents = MutableStateFlow<List<CloudStaffLoginEvent>>(emptyList())
    val staffLoginEvents: StateFlow<List<CloudStaffLoginEvent>> = _staffLoginEvents.asStateFlow()

    private val _typingStatuses = MutableStateFlow<List<CloudTypingStatus>>(emptyList())
    val typingStatuses: StateFlow<List<CloudTypingStatus>> = _typingStatuses.asStateFlow()

    @Volatile
    private var activeWorkspaceId: String? = null

    @Volatile
    private var activeMemberUids: List<String> = emptyList()

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required before cloud sync.")

    private fun isBootstrappedAdminEmail(email: String?, isVerified: Boolean): Boolean =
        email.equals(ADMIN_EMAIL, ignoreCase = true) && isVerified

    suspend fun connectForSignedInUser(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val email = user.email.orEmpty()
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').ifBlank { "Staff" }
        val isAdminEmail = isBootstrappedAdminEmail(email, user.isEmailVerified)

        _connectionState.value = WorkspaceConnectionState.Checking

        return try {
            localDb.ensureSeeded()
            ensureUserProfile(uid, displayName, email)

            // 1. Check user's bound workspace or any workspace containing this UID
            val userDoc = db.collection("users").document(uid).get().await()
            val savedWsId = userDoc.getString("activeWorkspaceId")?.takeIf { it.isNotBlank() }
            val cloudRole = UserRole.entries.firstOrNull { it.name == userDoc.getString("role") }
                ?: if (isAdminEmail) UserRole.ADMIN else UserRole.STAFF
            val linkedUsername = userDoc.getString("linkedStaffUsername").orEmpty()
            val linkedRole = UserRole.entries.firstOrNull { it.name == userDoc.getString("linkedStaffRole") }
            _loginMetadata.value = CloudLoginMetadata(
                cloudUserRole = cloudRole,
                linkedStaffUsername = linkedUsername,
                linkedStaffRole = linkedRole,
                verifiedInCloud = true
            )

            if (savedWsId != null) {
                val wsSnap = db.collection("workspaces").document(savedWsId).get().await()
                val ws = wsSnap.toObject(CloudWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (ws != null && (uid in ws.memberUids || ws.ownerUid == uid || isAdminEmail)) {
                    if (uid !in ws.memberUids && (ws.ownerUid == uid || isAdminEmail)) {
                        db.collection("workspaces").document(savedWsId).update(
                            mapOf(
                                "memberUids" to FieldValue.arrayUnion(uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    startWorkspaceListeners(savedWsId, uid, isAdminEmail)
                    return Result.success(Unit)
                }
            }

            // 2. Query workspaces where user is already a member
            val memberWsQuery = db.collection("workspaces")
                .whereArrayContains("memberUids", uid)
                .limit(1)
                .get()
                .await()
            val existingWsDoc = memberWsQuery.documents.firstOrNull()
            if (existingWsDoc != null) {
                val wsId = existingWsDoc.id
                updateUserWorkspaceBinding(uid, wsId)
                startWorkspaceListeners(wsId, uid, isAdminEmail)
                return Result.success(Unit)
            }

            // 3. If verified Admin email, auto-open or create shared company workspace
            if (isAdminEmail) {
                return createOrOpenCompanyWorkspace(
                    preferredWorkspaceId = DEFAULT_COMPANY_WORKSPACE_ID,
                    inviteCode = DEFAULT_INVITE_CODE,
                    uid = uid
                )
            }

            // 4. New staff phone / tester: let user enter company invite code OR start standalone workspace
            _connectionState.value = WorkspaceConnectionState.NeedsSetup(
                userEmail = email.ifBlank { displayName },
                isAdminEmail = false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "workspaces")
            _connectionState.value = WorkspaceConnectionState.Error(
                e.localizedMessage ?: "Unable to connect to cloud sync."
            )
            Result.failure(e)
        }
    }

    suspend fun joinWorkspaceWithInviteCode(rawInviteCode: String): Result<String> {
        val code = rawInviteCode.trim().uppercase()
        if (!ID_REGEX.matches(code) || code.length < 4 || code.length > 64) {
            return Result.failure(IllegalArgumentException("Enter a valid invite code (4–64 letters/numbers)."))
        }
        return try {
            val user = auth.currentUser ?: error("Sign in with Google first.")
            val uid = user.uid
            val isAdminEmail = isBootstrappedAdminEmail(user.email, user.isEmailVerified)

            val inviteSnap = db.collection("workspace_invites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invite code '$code' was not found."))
            }
            val invite = inviteSnap.toObject(CloudWorkspaceInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalArgumentException("Invalid invite record."))
            if (!invite.active) {
                return Result.failure(IllegalArgumentException("Invite code '$code' is no longer active."))
            }

            val wsId = invite.workspaceId
            db.collection("workspaces").document(wsId).update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "lastJoinInviteCode" to code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            updateUserWorkspaceBinding(uid, wsId)
            startWorkspaceListeners(wsId, uid, isAdminEmail)
            Result.success(wsId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "workspace_invites/$code")
            Result.failure(e)
        }
    }

    suspend fun createStandaloneOrCompanyWorkspace(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: error("Sign in with Google first.")
            val uid = user.uid
            val isAdminEmail = isBootstrappedAdminEmail(user.email, user.isEmailVerified)
            val wsId = if (isAdminEmail) DEFAULT_COMPANY_WORKSPACE_ID else "ws_${uid.take(16)}"
            val inviteCode = if (isAdminEmail) DEFAULT_INVITE_CODE else "GAS${uid.take(6).uppercase()}"
            createOrOpenCompanyWorkspace(wsId, inviteCode, uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "workspaces")
            Result.failure(e)
        }
    }

    private suspend fun createOrOpenCompanyWorkspace(
        preferredWorkspaceId: String,
        inviteCode: String,
        uid: String
    ): Result<Unit> {
        val user = auth.currentUser
        val isAdminEmail = isBootstrappedAdminEmail(user?.email, user?.isEmailVerified == true)
        val wsRef = db.collection("workspaces").document(preferredWorkspaceId)
        val snap = wsRef.get().await()

        if (!snap.exists()) {
            val wsData = mapOf(
                "companyName" to "C&A GAS",
                "ownerUid" to uid,
                "memberUids" to listOf(uid),
                "activeInviteCode" to inviteCode,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            wsRef.set(wsData).await()

            db.collection("workspace_invites").document(inviteCode).set(
                mapOf(
                    "workspaceId" to preferredWorkspaceId,
                    "ownerUid" to uid,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            seedInitialRoomDataToCloud(preferredWorkspaceId, listOf(uid), uid)
        } else {
            val ws = snap.toObject(CloudWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (ws != null && uid !in ws.memberUids && (ws.ownerUid == uid || isAdminEmail)) {
                wsRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        updateUserWorkspaceBinding(uid, preferredWorkspaceId)
        startWorkspaceListeners(preferredWorkspaceId, uid, isAdminEmail)
        return Result.success(Unit)
    }

    private suspend fun ensureUserProfile(uid: String, displayName: String, email: String) {
        val ref = db.collection("users").document(uid)
        val existing = ref.get().await()
        val user = auth.currentUser
        val isAdminEmail = isBootstrappedAdminEmail(email, user?.isEmailVerified == true)
        val defaultRole = if (isAdminEmail) UserRole.ADMIN.name else UserRole.STAFF.name
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to displayName.take(100),
                "email" to email.take(200),
                "role" to defaultRole,
                "activeWorkspaceId" to "",
                "linkedStaffUsername" to "",
                "linkedStaffRole" to "",
                "autoDriveBackupEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.take(100),
                "email" to email.take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existingRole = existing.getString("role")
            if (isAdminEmail && existingRole != UserRole.ADMIN.name) {
                updates["role"] = UserRole.ADMIN.name
            } else if (existingRole.isNullOrBlank()) {
                updates["role"] = UserRole.STAFF.name
            }
            ref.update(updates).await()
        }
    }

    private suspend fun updateUserWorkspaceBinding(uid: String, workspaceId: String) {
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "activeWorkspaceId" to workspaceId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        }
    }

    private suspend fun seedInitialRoomDataToCloud(
        workspaceId: String,
        memberUids: List<String>,
        uid: String
    ) {
        localDb.ensureSeeded()
        val wsRef = db.collection("workspaces").document(workspaceId)
        val users = localDb.userDao().getAllUsersOnce()
        val vehicles = localDb.vehicleDao().getAllVehiclesOnce()
        val categories = localDb.productCategoryDao().getAllOnce()
        val products = localDb.productDao().getAllProductsOnce()
        val openings = localDb.openingStockDao().getAllOpeningsOnce()
        val txs = localDb.transactionDao().getAllTransactionsOnce()

        val batch = db.batch()
        users.forEach { u ->
            val docRef = wsRef.collection("staff_accounts").document("user_${u.username}")
            batch.set(docRef, CloudStaffAccount.toCreateMap(u, memberUids, uid))
        }
        vehicles.forEach { v ->
            val docRef = wsRef.collection("vehicles").document("veh_${v.id}")
            batch.set(docRef, CloudVehicle.toCreateMap(v, memberUids, uid))
        }
        categories.forEach { c ->
            val docRef = wsRef.collection("product_categories").document("cat_${c.id}")
            batch.set(docRef, CloudProductCategory.toCreateMap(c, memberUids, uid))
        }
        products.forEach { p ->
            val docRef = wsRef.collection("products").document("prod_${p.id}")
            batch.set(docRef, CloudProduct.toCreateMap(p, memberUids, uid))
        }
        openings.forEach { o ->
            val docId = CloudOpeningStock.docIdFor(o.scope, o.vehicleId, o.productId)
            val docRef = wsRef.collection("openings").document(docId)
            batch.set(docRef, CloudOpeningStock.toCreateMap(o, memberUids, uid))
        }
        batch.commit().await()

        for (tx in txs) {
            val remoteId = tx.remoteId ?: generateRemoteTxId()
            if (tx.remoteId == null) {
                localDb.transactionDao().updateRemoteId(tx.id, remoteId)
            }
            wsRef.collection("transactions").document(remoteId)
                .set(CloudStockTransaction.toCreateMap(tx, remoteId, memberUids, uid))
                .await()
        }
    }

    private fun startWorkspaceListeners(workspaceId: String, uid: String, isAdminEmail: Boolean) {
        activeSyncJob?.cancel()
        activeWorkspaceId = workspaceId

        val wsRef = db.collection("workspaces").document(workspaceId)

        activeSyncJob = syncScope.launch {
            // Listener 1: Workspace document (membership + invite code)
            launch {
                wsRef.snapshots()
                    .map { snap -> snap.toObject(CloudWorkspace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        if (auth.currentUser?.uid == uid) {
                            handleFirestoreError(fsError ?: error, OperationType.GET, wsRef.path)
                            _connectionState.value = WorkspaceConnectionState.Error(
                                (fsError ?: error).localizedMessage ?: "Workspace sync error"
                            )
                        }
                    }
                    .collectLatest { ws ->
                        if (ws != null) {
                            val previousMembers = activeMemberUids
                            activeMemberUids = ws.memberUids
                            val isOwnerOrAdmin = ws.ownerUid == uid ||
                                isAdminEmail ||
                                _loginMetadata.value.cloudUserRole == UserRole.ADMIN
                            _connectionState.value = WorkspaceConnectionState.Connected(
                                workspaceId = workspaceId,
                                companyName = ws.companyName,
                                inviteCode = ws.activeInviteCode,
                                memberCount = ws.memberUids.size,
                                isOwnerOrAdmin = isOwnerOrAdmin,
                                lastSyncNote = "Live Online Sync Active"
                            )
                            if (ws.memberUids != previousMembers && ws.memberUids.isNotEmpty() && isOwnerOrAdmin) {
                                propagateMemberUidsAcrossWorkspace(workspaceId, ws.memberUids)
                            }
                            syncPendingLocalTransactions()
                        }
                    }
            }

            // Admin & Owner Executive listeners: Reports, Login Events, Security & Devices
            val isPrivilegedAdmin = isAdminEmail || _loginMetadata.value.cloudUserRole == UserRole.ADMIN
            val isExecutiveReader = isPrivilegedAdmin || _loginMetadata.value.cloudUserRole == UserRole.OWNER_VIEW
            if (isPrivilegedAdmin) {
                launch {
                    val settingsRef = wsRef.collection("admin_settings").document("drive_backup")
                    settingsRef.snapshots()
                        .map { snap ->
                            snap.toObject(
                                CloudAdminSettings::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .catch { error -> logListenerErrorIfAuthenticated(error, uid, settingsRef.path) }
                        .collectLatest { settings ->
                            _adminSettings.value = settings
                        }
                }
            } else {
                _adminSettings.value = null
            }

            if (isExecutiveReader) {
                launch {
                    val reportsCol = wsRef.collection("admin_reports")
                    reportsCol.snapshots()
                        .map { snap ->
                            snap.toObjects(
                                CloudAdminReportSnapshot::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .catch { error -> logListenerErrorIfAuthenticated(error, uid, reportsCol.path) }
                        .collectLatest { reports ->
                            _adminReports.value = reports
                        }
                }
                launch {
                    val loginEventsCol = wsRef.collection("staff_login_events")
                    loginEventsCol.snapshots()
                        .map { snap ->
                            snap.toObjects(
                                CloudStaffLoginEvent::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            ).sortedByDescending { it.loginTimestampMs }
                                .take(50)
                        }
                        .catch { error -> logListenerErrorIfAuthenticated(error, uid, loginEventsCol.path) }
                        .collectLatest { events ->
                            _staffLoginEvents.value = events
                        }
                }
                launch {
                    val mobilesCol = wsRef.collection("authorized_mobiles")
                    mobilesCol.snapshots()
                        .catch { error -> logListenerErrorIfAuthenticated(error, uid, mobilesCol.path) }
                        .collectLatest { snap ->
                            localDb.withTransaction {
                                snap.documents.forEach { doc ->
                                    val phone = doc.getString("phoneE164").orEmpty()
                                    val name = doc.getString("displayName").orEmpty()
                                    val role = UserRole.entries.firstOrNull { it.name == doc.getString("role") } ?: UserRole.STAFF
                                    if (phone.isNotBlank() && name.isNotBlank()) {
                                        val existing = localDb.authorizedMobileDao().getByPhone(phone)
                                        val entity = AuthorizedMobileEntity(
                                            id = existing?.id ?: 0L,
                                            phoneE164 = phone,
                                            displayName = name,
                                            role = role,
                                            staffUserId = doc.getLong("staffUserId"),
                                            allowedFeaturesCsv = doc.getString("allowedFeaturesCsv") ?: StaffFeaturePermission.ALL_DEFAULT_CSV,
                                            isActive = doc.getBoolean("isActive") ?: true,
                                            isProtectedOwner = doc.getBoolean("isProtectedOwner") ?: false,
                                            createdAtMs = doc.getLong("createdAtMs") ?: System.currentTimeMillis(),
                                            updatedAtMs = doc.getLong("updatedAtMs") ?: System.currentTimeMillis(),
                                            firstTimeRegEnabled = doc.getBoolean("firstTimeRegEnabled") ?: existing?.firstTimeRegEnabled ?: true,
                                            isRegistered = doc.getBoolean("isRegistered") ?: existing?.isRegistered ?: false,
                                            otpVerifiedAtMs = doc.getLong("otpVerifiedAtMs") ?: existing?.otpVerifiedAtMs ?: 0L,
                                            registeredDeviceId = doc.getString("registeredDeviceId") ?: existing?.registeredDeviceId.orEmpty(),
                                            failedLoginAttempts = existing?.failedLoginAttempts ?: 0,
                                            lockedUntilMs = existing?.lockedUntilMs ?: 0L
                                        )
                                        if (existing == null || entity.updatedAtMs >= existing.updatedAtMs) {
                                            localDb.authorizedMobileDao().upsert(entity)
                                        }
                                    }
                                }
                            }
                        }
                }
                launch {
                    val devicesCol = wsRef.collection("registered_devices")
                    devicesCol.snapshots()
                        .catch { error -> logListenerErrorIfAuthenticated(error, uid, devicesCol.path) }
                        .collectLatest { snap ->
                            localDb.withTransaction {
                                snap.documents.forEach { doc ->
                                    val devId = doc.getString("deviceId").orEmpty()
                                    val devLabel = doc.getString("deviceLabel").orEmpty()
                                    val role = UserRole.entries.firstOrNull { it.name == doc.getString("role") } ?: UserRole.STAFF
                                    if (devId.isNotBlank()) {
                                        val existing = localDb.registeredDeviceDao().getByDeviceId(devId)
                                        val entity = RegisteredDeviceEntity(
                                            id = existing?.id ?: 0L,
                                            deviceId = devId,
                                            deviceLabel = devLabel.ifBlank { "Android Device" },
                                            phoneE164 = doc.getString("phoneE164").orEmpty(),
                                            userId = doc.getLong("userId") ?: 0L,
                                            displayName = doc.getString("displayName").orEmpty().ifBlank { "User" },
                                            role = role,
                                            sessionVersion = doc.getLong("sessionVersion") ?: 1L,
                                            isRevoked = doc.getBoolean("isRevoked") ?: false,
                                            lastActiveMs = doc.getLong("lastActiveMs") ?: System.currentTimeMillis(),
                                            registeredAtMs = doc.getLong("registeredAtMs") ?: System.currentTimeMillis()
                                        )
                                        if (existing == null || entity.sessionVersion >= existing.sessionVersion) {
                                            localDb.registeredDeviceDao().upsert(entity)
                                        }
                                    }
                                }
                            }
                        }
                }
            } else {
                _adminReports.value = emptyList()
                _staffLoginEvents.value = emptyList()
            }

            // Listener 2: Staff & Admin accounts
            launch {
                val path = "${wsRef.path}/staff_accounts"
                wsRef.collection("staff_accounts")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudStaffAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudUsers ->
                        if (cloudUsers.isNotEmpty()) {
                            localDb.withTransaction {
                                val existingByUsername = localDb.userDao().getAllUsersOnce().associateBy { it.username }
                                cloudUsers.forEach { cu ->
                                    val existing = existingByUsername[cu.username]
                                    if (existing != null) {
                                        val incomingHash = cu.resolvedPinHash()
                                        if (existing.pinHash != incomingHash || existing.pin.isNotEmpty() || existing.displayName != cu.displayName || existing.isActive != cu.active) {
                                            localDb.userDao().insert(
                                                existing.copy(
                                                    displayName = cu.displayName,
                                                    pin = "",
                                                    pinHash = incomingHash.ifBlank { existing.pinHash },
                                                    isActive = cu.active
                                                )
                                            )
                                        }
                                    } else {
                                        localDb.userDao().insert(cu.toEntity())
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 3: Vehicles
            launch {
                val path = "${wsRef.path}/vehicles"
                wsRef.collection("vehicles")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudVehicles ->
                        if (cloudVehicles.isNotEmpty()) {
                            localDb.withTransaction {
                                val existing = localDb.vehicleDao().getAllVehiclesOnce()
                                val byId = existing.associateBy { it.id }
                                val byStaffId = existing.associateBy { it.staffUserId }
                                val byName = existing.associateBy { it.name.lowercase() }
                                cloudVehicles.forEach { cv ->
                                    val match = byId[cv.localId]
                                        ?: byStaffId[cv.staffUserId]
                                        ?: byName[cv.name.lowercase()]
                                    if (match != null) {
                                        val updated = match.copy(
                                            name = cv.name,
                                            staffUserId = cv.staffUserId,
                                            isActive = cv.active
                                        )
                                        if (updated != match) {
                                            localDb.vehicleDao().insert(updated)
                                        }
                                    } else {
                                        localDb.vehicleDao().insert(cv.toEntity())
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 4: Products
            launch {
                val path = "${wsRef.path}/products"
                wsRef.collection("products")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudProducts ->
                        if (cloudProducts.isNotEmpty()) {
                            localDb.withTransaction {
                                val existing = localDb.productDao().getAllProductsOnce()
                                val byId = existing.associateBy { it.id }
                                val byKey = existing.associateBy { it.category.name to it.name.lowercase() }
                                cloudProducts.forEach { cp ->
                                    val match = byId[cp.localId] ?: byKey[cp.category to cp.name.lowercase()]
                                    if (match != null) {
                                        val parsedCat = com.example.data.model.ProductCategory.entries
                                            .firstOrNull { it.name == cp.category } ?: match.category
                                        val updated = match.copy(
                                            category = parsedCat,
                                            categoryId = if (cp.categoryId > 0L) cp.categoryId else match.categoryId,
                                            name = cp.name,
                                            sortOrder = cp.sortOrder,
                                            isActive = cp.active,
                                            cylinderSizeKg = cp.cylinderSizeKg.ifBlank { match.cylinderSizeKg },
                                            unit = cp.unit.ifBlank { match.unit },
                                            description = cp.description.ifBlank { match.description },
                                            iconKey = cp.iconKey.ifBlank { match.iconKey }
                                        )
                                        if (updated != match) {
                                            localDb.productDao().update(updated)
                                        }
                                    } else {
                                        localDb.productDao().insertAll(listOf(cp.toEntity()))
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 4B: Dynamic Product Category Folders
            launch {
                val path = "${wsRef.path}/product_categories"
                wsRef.collection("product_categories")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudProductCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudCats ->
                        if (cloudCats.isNotEmpty()) {
                            localDb.withTransaction {
                                val existingById = localDb.productCategoryDao().getAllOnce().associateBy { it.id }
                                cloudCats.forEach { cc ->
                                    val entity = cc.toEntity()
                                    val match = existingById[entity.id]
                                    if (match != null) {
                                        val updated = match.copy(
                                            name = entity.name,
                                            description = entity.description,
                                            iconKey = entity.iconKey,
                                            categoryType = entity.categoryType,
                                            legacyEnum = entity.legacyEnum,
                                            sortOrder = entity.sortOrder,
                                            isActive = entity.isActive
                                        )
                                        if (updated != match) {
                                            localDb.productCategoryDao().update(updated)
                                        }
                                    } else {
                                        localDb.productCategoryDao().insert(entity)
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 5: Opening Stocks
            launch {
                val path = "${wsRef.path}/openings"
                wsRef.collection("openings")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudOpeningStock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudOpenings ->
                        if (cloudOpenings.isNotEmpty()) {
                            localDb.withTransaction {
                                cloudOpenings.forEach { co ->
                                    val existing = localDb.openingStockDao().getOpening(co.scope, co.vehicleId, co.productId)
                                    val entity = co.toEntity(existing?.id ?: 0L)
                                    if (existing == null ||
                                        existing.openingFull != entity.openingFull ||
                                        existing.openingEmpty != entity.openingEmpty
                                    ) {
                                        localDb.openingStockDao().upsert(entity)
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 6: Online Transactions (from all 7 staff + Admin)
            launch {
                val path = "${wsRef.path}/transactions"
                wsRef.collection("transactions")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { snapshot ->
                        val fromServer = !snapshot.metadata.isFromCache
                        val cloudTxs = snapshot.toObjects(
                            CloudStockTransaction::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        val cloudIds = cloudTxs.mapNotNull { it.clientTxId.ifBlank { it.id }.takeIf { s -> s.isNotBlank() } }.toSet()

                        localDb.withTransaction {
                            val existingTxs = localDb.transactionDao().getAllTransactionsOnce()
                            val existingByRemoteId = existingTxs
                                .mapNotNull { tx -> tx.remoteId?.let { r -> r to tx } }
                                .toMap()

                            for (ct in cloudTxs) {
                                val entity = ct.toEntity() ?: continue
                                val slug = entity.remoteId ?: continue
                                val existing = existingByRemoteId[slug]
                                if (existing == null) {
                                    localDb.transactionDao().insertIgnore(entity)
                                } else if (existing.productId != entity.productId ||
                                    existing.vehicleId != entity.vehicleId ||
                                    existing.vehicleName != entity.vehicleName ||
                                    existing.quantity != entity.quantity ||
                                    existing.purchaseCompanyName != entity.purchaseCompanyName ||
                                    existing.customerName != entity.customerName ||
                                    existing.notes != entity.notes
                                ) {
                                    localDb.transactionDao().update(
                                        existing.copy(
                                            productId = entity.productId,
                                            vehicleId = entity.vehicleId,
                                            vehicleName = entity.vehicleName,
                                            quantity = entity.quantity,
                                            purchaseCompanyName = entity.purchaseCompanyName,
                                            customerName = entity.customerName,
                                            notes = entity.notes
                                        )
                                    )
                                }
                            }

                            if (fromServer) {
                                for (localTx in existingTxs) {
                                    val rId = localTx.remoteId ?: continue
                                    if (rId !in cloudIds) {
                                        localDb.transactionDao().deleteByRemoteId(rId)
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 7: Staff Daily Duty Attendance
            launch {
                val path = "${wsRef.path}/staff_attendance"
                wsRef.collection("staff_attendance")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudStaffAttendance::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudRows ->
                        if (cloudRows.isNotEmpty()) {
                            localDb.withTransaction {
                                cloudRows.forEach { row ->
                                    val rId = row.attendanceId.ifBlank { row.id }
                                    if (rId.isNotBlank()) {
                                        val existing = localDb.staffAttendanceDao().getByRemoteId(rId)
                                            ?: localDb.staffAttendanceDao().getForStaffAndDate(row.businessDate, row.staffId)
                                        val entity = row.toEntity(existing?.id ?: 0L)
                                        if (existing == null || entity.updatedAtMs >= existing.updatedAtMs) {
                                            localDb.staffAttendanceDao().upsert(entity)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 8: Pending Store-to-Vehicle Full & Vehicle-to-Store Empty Approvals
            launch {
                val path = "${wsRef.path}/pending_store_transfers"
                wsRef.collection("pending_store_transfers")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudPendingStoreTransfer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudRequests ->
                        if (cloudRequests.isNotEmpty()) {
                            localDb.withTransaction {
                                cloudRequests.forEach { req ->
                                    val rId = req.requestId.ifBlank { req.id }
                                    if (rId.isNotBlank()) {
                                        val existing = localDb.pendingStoreTransferDao().getByRemoteId(rId)
                                        val entity = req.toEntity(existing?.id ?: 0L)
                                        if (existing == null || existing != entity) {
                                            localDb.pendingStoreTransferDao().upsert(entity)
                                        }
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 9: Universal App Bell Notifications
            launch {
                val path = "${wsRef.path}/app_notifications"
                wsRef.collection("app_notifications")
                    .whereArrayContains("memberUids", uid)
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudAppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudNotifs ->
                        if (cloudNotifs.isNotEmpty()) {
                            localDb.withTransaction {
                                cloudNotifs.forEach { cn ->
                                    val rId = cn.notifId.ifBlank { cn.id }
                                    if (rId.isNotBlank()) {
                                        val existing = localDb.appNotificationDao().getByRemoteId(rId)
                                        if (existing == null) {
                                            localDb.appNotificationDao().insertIgnore(cn.toEntity(0L, existingRead = false))
                                        }
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 10: Team & Admin-Owner Chat Messages
            launch {
                val path = "${wsRef.path}/chat_messages"
                val baseQuery = wsRef.collection("chat_messages")
                    .whereArrayContains("memberUids", uid)
                val query = if (isPrivilegedAdmin) {
                    baseQuery
                } else {
                    baseQuery.whereEqualTo("channel", "TEAM_CHAT")
                }
                query.snapshots()
                    .map { snap -> snap.toObjects(CloudChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { cloudMsgs ->
                        if (cloudMsgs.isNotEmpty()) {
                            localDb.withTransaction {
                                cloudMsgs.forEach { cm ->
                                    val rId = cm.messageId.ifBlank { cm.id }
                                    if (rId.isNotBlank()) {
                                        val existing = localDb.chatMessageDao().getByRemoteId(rId)
                                        if (existing == null) {
                                            localDb.chatMessageDao().insertIgnore(cm.toEntity())
                                        }
                                    }
                                }
                            }
                        }
                    }
            }

            // Listener 11: Live Chat Typing Statuses
            launch {
                val path = "${wsRef.path}/typing_status"
                wsRef.collection("typing_status")
                    .snapshots()
                    .map { snap -> snap.toObjects(CloudTypingStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                    .catch { error -> logListenerErrorIfAuthenticated(error, uid, path) }
                    .collectLatest { statuses ->
                        _typingStatuses.value = statuses
                    }
            }
        }
    }

    private fun logListenerErrorIfAuthenticated(error: Throwable, uid: String, path: String) {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsError ?: error, OperationType.LIST, path)
        }
    }

    private suspend fun propagateMemberUidsAcrossWorkspace(workspaceId: String, memberUids: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val wsRef = db.collection("workspaces").document(workspaceId)
        val subcollections = listOf("staff_accounts", "vehicles", "products", "openings", "transactions")
        for (col in subcollections) {
            try {
                val docs = wsRef.collection(col).whereArrayContains("memberUids", uid).get().await()
                val chunks = docs.documents.filter { doc ->
                    val existingMembers = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    existingMembers != memberUids
                }.chunked(400)
                for (chunk in chunks) {
                    val batch = db.batch()
                    chunk.forEach { doc ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberUids" to memberUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "${wsRef.path}/$col")
            }
        }
    }

    suspend fun pushTransaction(tx: StockTransactionEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val remoteId = tx.remoteId ?: return Result.failure(IllegalStateException("Missing remoteId"))
        val docPath = "workspaces/$wsId/transactions/$remoteId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val payload = CloudStockTransaction.toCreateMap(
                entity = tx.copy(
                    purchaseCompanyName = tx.purchaseCompanyName.take(120),
                    customerName = tx.customerName.take(120),
                    notes = tx.notes.take(300)
                ),
                remoteTxId = remoteId,
                memberUids = members,
                uid = uid
            )
            db.collection("workspaces").document(wsId)
                .collection("transactions").document(remoteId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushTransactionUpdate(tx: StockTransactionEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val remoteId = tx.remoteId ?: return pushTransaction(tx.copy(remoteId = generateRemoteTxId()))
        val docPath = "workspaces/$wsId/transactions/$remoteId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("transactions").document(remoteId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "productId" to tx.productId,
                    "quantity" to tx.quantity,
                    "purchaseCompanyName" to tx.purchaseCompanyName.take(120),
                    "customerName" to tx.customerName.take(120),
                    "notes" to tx.notes.take(300),
                    "memberUids" to members,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.vehicleId?.let { updates["vehicleId"] = it }
                tx.vehicleName?.takeIf { it.isNotBlank() }?.let { updates["vehicleName"] = it.take(64) }
                docRef.update(updates).await()
            } else {
                val payload = CloudStockTransaction.toCreateMap(
                    entity = tx.copy(
                        purchaseCompanyName = tx.purchaseCompanyName.take(120),
                        customerName = tx.customerName.take(120),
                        notes = tx.notes.take(300)
                    ),
                    remoteTxId = remoteId,
                    memberUids = members,
                    uid = uid
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudTransaction(remoteId: String): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docPath = "workspaces/$wsId/transactions/$remoteId"
        return try {
            requireUserId()
            db.collection("workspaces").document(wsId)
                .collection("transactions").document(remoteId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushOpeningStock(opening: OpeningStockEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = CloudOpeningStock.docIdFor(opening.scope, opening.vehicleId, opening.productId)
        val docPath = "workspaces/$wsId/openings/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId).collection("openings").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "openingFull" to opening.openingFull,
                        "openingEmpty" to opening.openingEmpty,
                        "updatedBy" to opening.updatedBy.take(100),
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudOpeningStock.toCreateMap(opening, members, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushProduct(product: ProductEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = "prod_${product.id}"
        val docPath = "workspaces/$wsId/products/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId).collection("products").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "category" to product.category.name,
                        "categoryId" to product.categoryId,
                        "name" to product.name.take(100),
                        "sortOrder" to product.sortOrder,
                        "active" to product.isActive,
                        "cylinderSizeKg" to product.cylinderSizeKg.take(40),
                        "unit" to product.unit.take(30),
                        "description" to product.description.take(240),
                        "iconKey" to product.iconKey.take(40),
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudProduct.toCreateMap(product, members, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushProductCategory(category: ProductCategoryEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = "cat_${category.id}"
        val docPath = "workspaces/$wsId/product_categories/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId).collection("product_categories").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to category.name.take(100),
                        "description" to category.description.take(240),
                        "iconKey" to category.iconKey.take(40),
                        "categoryType" to category.categoryType.take(40),
                        "legacyEnum" to category.legacyEnum.name,
                        "sortOrder" to category.sortOrder,
                        "active" to category.isActive,
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudProductCategory.toCreateMap(category, members, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: Long): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = "prod_$productId"
        val docPath = "workspaces/$wsId/products/$docId"
        return try {
            requireUserId()
            db.collection("workspaces").document(wsId).collection("products").document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushUserPin(user: UserAccountEntity): Result<Unit> =
        pushStaffAccount(user)

    suspend fun pushStaffAccount(user: UserAccountEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = "user_${user.username}"
        val docPath = "workspaces/$wsId/staff_accounts/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId).collection("staff_accounts").document(docId)
            val snap = docRef.get().await()
            val safeHash = user.pinHash.ifBlank {
                if (user.pin.isNotBlank()) AdminCredentialCrypto.hashPassword(user.pin) else ""
            }
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "pin" to "",
                        "pinHash" to safeHash,
                        "displayName" to user.displayName.take(100),
                        "active" to user.isActive,
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudStaffAccount.toCreateMap(user.copy(pin = "", pinHash = safeHash), members, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushVehicle(vehicle: VehicleEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = "veh_${vehicle.id}"
        val docPath = "workspaces/$wsId/vehicles/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId).collection("vehicles").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to vehicle.name.take(64),
                        "staffUserId" to vehicle.staffUserId,
                        "active" to vehicle.isActive,
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudVehicle.toCreateMap(vehicle, members, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncPendingLocalTransactions(): Result<Int> {
        val wsId = activeWorkspaceId ?: return Result.success(0)
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val unsynced = localDb.transactionDao().getUnsyncedTransactionsOnce()
            var count = 0
            for (tx in unsynced) {
                val remoteId = generateRemoteTxId()
                db.collection("workspaces").document(wsId)
                    .collection("transactions").document(remoteId)
                    .set(CloudStockTransaction.toCreateMap(tx, remoteId, members, uid))
                    .await()
                localDb.transactionDao().updateRemoteId(tx.id, remoteId)
                count++
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "workspaces/$wsId/transactions")
            Result.failure(e)
        }
    }

    suspend fun pushAllLocalDataToCloud(): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.failure(IllegalStateException("Not connected to workspace"))
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            seedInitialRoomDataToCloud(wsId, members, uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "workspaces/$wsId")
            Result.failure(e)
        }
    }

    /**
     * Validates an Admin or Staff account PIN & role against Firestore (`staff_accounts`),
     * checks global PIN uniqueness across workspace accounts, syncs refreshed account details
     * to local Room, and updates `/users/{uid}` metadata (`linkedStaffUsername`, `linkedStaffRole`).
     * Falls back cleanly to local cache when the device is offline or workspace sync is unconfigured.
     */
    suspend fun verifyRolePinAndSyncProfile(
        localUser: UserAccountEntity,
        expectedRole: UserRole,
        pin: String
    ): Result<UserAccountEntity> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required before unlocking an account."))

        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }
        if (localUser.role != expectedRole) {
            return Result.failure(
                IllegalArgumentException("${localUser.displayName} is ${localUser.role.name}, not ${expectedRole.name}.")
            )
        }
        if (!localUser.isActive) {
            return Result.failure(IllegalStateException("Account '${localUser.displayName}' is currently inactive."))
        }

        val currentConn = _connectionState.value
        if (expectedRole == UserRole.ADMIN &&
            currentConn is WorkspaceConnectionState.Connected &&
            !currentConn.isOwnerOrAdmin
        ) {
            return Result.failure(
                SecurityException("Admin access restricted: your Google user role is STAFF. Sign in under Staff or use Admin Google account.")
            )
        }

        val wsId = activeWorkspaceId
        var verifiedUser = localUser

        if (wsId != null) {
            val staffCol = db.collection("workspaces").document(wsId).collection("staff_accounts")
            val cloudAccounts: List<CloudStaffAccount>? = try {
                val serverSnap = staffCol
                    .whereArrayContains("memberUids", uid)
                    .get(Source.SERVER)
                    .await()
                serverSnap.toObjects(CloudStaffAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val fsErr = generateSequence<Throwable>(e) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    fsErr?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
                ) {
                    // Offline: attempt cached Firestore read before falling back to Room
                    try {
                        val cacheSnap = staffCol
                            .whereArrayContains("memberUids", uid)
                            .get(Source.CACHE)
                            .await()
                        cacheSnap.toObjects(CloudStaffAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            .takeIf { it.isNotEmpty() }
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    handleFirestoreError(e, OperationType.LIST, "workspaces/$wsId/staff_accounts")
                    null
                }
            }

            if (!cloudAccounts.isNullOrEmpty()) {
                val cloudMatch = cloudAccounts.firstOrNull { it.username == localUser.username }
                if (cloudMatch != null) {
                    if (!cloudMatch.active) {
                        localDb.userDao().updateIsActive(localUser.id, false)
                        return Result.failure(IllegalStateException("Account '${cloudMatch.displayName}' has been deactivated."))
                    }
                    if (cloudMatch.role != expectedRole.name) {
                        return Result.failure(
                            IllegalArgumentException("Role mismatch: Firestore lists '${cloudMatch.displayName}' as ${cloudMatch.role}.")
                        )
                    }
                    // Keep Room synchronized with Firestore's authoritative hashed PIN & displayName
                    val cloudHash = cloudMatch.resolvedPinHash()
                    if (cloudHash.isNotBlank() && (cloudHash != localUser.pinHash || localUser.pin.isNotEmpty() || cloudMatch.displayName != localUser.displayName)) {
                        verifiedUser = localUser.copy(
                            displayName = cloudMatch.displayName,
                            pin = "",
                            pinHash = cloudHash,
                            isActive = cloudMatch.active
                        )
                        localDb.userDao().insert(verifiedUser)
                    }
                    if (!cloudMatch.matchesPin(pin)) {
                        return Result.failure(
                            IllegalArgumentException("Invalid PIN for ${expectedRole.name} '${cloudMatch.displayName}'.")
                        )
                    }
                    val duplicateOther = cloudAccounts.firstOrNull {
                        it.active && it.username != cloudMatch.username && it.matchesPin(pin)
                    }
                    if (duplicateOther != null) {
                        return Result.failure(
                            IllegalStateException(
                                "This PIN conflicts with ${duplicateOther.displayName}. Ask Admin to assign unique PINs."
                            )
                        )
                    }
                }
            }
        }

        // Persist linked role & staff metadata on Firebase user's /users/{uid} doc
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "linkedStaffUsername" to verifiedUser.username,
                    "linkedStaffRole" to expectedRole.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            _loginMetadata.value = _loginMetadata.value.copy(
                linkedStaffUsername = verifiedUser.username,
                linkedStaffRole = expectedRole,
                verifiedInCloud = wsId != null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        }

        return Result.success(verifiedUser)
    }

    suspend fun saveAdminDriveSettings(
        autoDriveBackupEnabled: Boolean,
        lastDriveBackupAtMs: Long,
        lastDriveFileId: String,
        lastDriveBackupStatus: String,
        themePreset: String,
        autoLogoutMode: String
    ): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val conn = _connectionState.value
        if (conn is WorkspaceConnectionState.Connected && !conn.isOwnerOrAdmin) {
            return Result.failure(SecurityException("Restricted: Only Admin can update workspace Drive settings."))
        }
        val docPath = "workspaces/$wsId/admin_settings/drive_backup"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email.orEmpty().take(200)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("admin_settings").document("drive_backup")
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "autoDriveBackupEnabled" to autoDriveBackupEnabled,
                        "lastDriveBackupAtMs" to lastDriveBackupAtMs.coerceAtLeast(0L),
                        "lastDriveFileId" to lastDriveFileId.take(256),
                        "lastDriveBackupStatus" to lastDriveBackupStatus.take(300),
                        "driveAccountEmail" to email,
                        "themePreset" to themePreset.take(64),
                        "autoLogoutMode" to autoLogoutMode.take(64),
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "settingId" to "drive_backup",
                        "autoDriveBackupEnabled" to autoDriveBackupEnabled,
                        "lastDriveBackupAtMs" to lastDriveBackupAtMs.coerceAtLeast(0L),
                        "lastDriveFileId" to lastDriveFileId.take(256),
                        "lastDriveBackupStatus" to lastDriveBackupStatus.take(300),
                        "driveAccountEmail" to email,
                        "themePreset" to themePreset.take(64),
                        "autoLogoutMode" to autoLogoutMode.take(64),
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "autoDriveBackupEnabled" to autoDriveBackupEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAdminCredentialsToCloud(
        adminLoginUsername: String,
        adminPasswordHash: String,
        adminRecoveryEmail: String
    ): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val conn = _connectionState.value
        if (conn is WorkspaceConnectionState.Connected && !conn.isOwnerOrAdmin) {
            return Result.failure(SecurityException("Restricted: Only Admin can update Admin login credentials."))
        }
        val docPath = "workspaces/$wsId/admin_settings/drive_backup"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email.orEmpty().take(200)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("admin_settings").document("drive_backup")
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "adminLoginUsername" to adminLoginUsername.trim().take(64),
                        "adminPasswordHash" to adminPasswordHash.take(256),
                        "adminRecoveryEmail" to adminRecoveryEmail.trim().lowercase().take(200),
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "settingId" to "drive_backup",
                        "autoDriveBackupEnabled" to false,
                        "lastDriveBackupAtMs" to 0L,
                        "lastDriveFileId" to "",
                        "lastDriveBackupStatus" to "Google Drive backup ready to connect",
                        "driveAccountEmail" to email,
                        "adminLoginUsername" to adminLoginUsername.trim().take(64),
                        "adminPasswordHash" to adminPasswordHash.take(256),
                        "adminRecoveryEmail" to adminRecoveryEmail.trim().lowercase().take(200),
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

    suspend fun saveOwnerViewPasswordHashToCloud(
        ownerViewPasswordHash: String
    ): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val conn = _connectionState.value
        if (conn is WorkspaceConnectionState.Connected && !conn.isOwnerOrAdmin) {
            return Result.failure(SecurityException("Restricted: Only Admin can update the Owner View password."))
        }
        val docPath = "workspaces/$wsId/admin_settings/drive_backup"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email.orEmpty().take(200)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("admin_settings").document("drive_backup")
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "ownerViewPasswordHash" to ownerViewPasswordHash.take(256),
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "settingId" to "drive_backup",
                        "autoDriveBackupEnabled" to false,
                        "lastDriveBackupAtMs" to 0L,
                        "lastDriveFileId" to "",
                        "lastDriveBackupStatus" to "Google Drive backup ready to connect",
                        "driveAccountEmail" to email,
                        "ownerViewPasswordHash" to ownerViewPasswordHash.take(256),
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

    suspend fun recordStaffLoginEvent(
        staffId: Long,
        staffName: String,
        vehicleName: String,
        loginTimestampMs: Long = System.currentTimeMillis()
    ): Result<String> {
        val wsId = activeWorkspaceId ?: return Result.success("local_only")
        val eventId = "login_${loginTimestampMs}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val docPath = "workspaces/$wsId/staff_login_events/$eventId"
        return try {
            val uid = requireUserId()
            val payload = buildMap<String, Any> {
                put("eventId", eventId)
                put("staffId", staffId.coerceAtLeast(1L))
                put("staffName", staffName.trim().take(100).ifBlank { "Staff" })
                val safeVehicle = vehicleName.trim().take(64)
                if (safeVehicle.isNotEmpty()) {
                    put("vehicleName", safeVehicle)
                }
                put("loginTimestampMs", loginTimestampMs.coerceAtLeast(1L))
                put("loginByUid", uid)
                put("createdAt", FieldValue.serverTimestamp())
            }
            db.collection("workspaces").document(wsId)
                .collection("staff_login_events")
                .document(eventId)
                .set(payload)
                .await()
            Result.success(eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAdminReportSnapshot(
        reportId: String,
        reportType: String,
        reportTitle: String,
        subtitle: String,
        businessDate: String,
        yearMonth: String,
        generatedBy: String,
        rowCount: Int,
        summaryTotals: String,
        driveFileId: String = ""
    ): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.failure(IllegalStateException("Not connected to cloud workspace."))
        val conn = _connectionState.value
        if (conn is WorkspaceConnectionState.Connected && !conn.isOwnerOrAdmin) {
            return Result.failure(SecurityException("Restricted: Only Admin can access or save company reports."))
        }
        val cleanId = reportId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docPath = "workspaces/$wsId/admin_reports/$cleanId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("workspaces").document(wsId)
                .collection("admin_reports").document(cleanId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "reportTitle" to reportTitle.take(160).ifBlank { reportType },
                        "subtitle" to subtitle.take(300),
                        "rowCount" to rowCount.coerceAtLeast(0),
                        "summaryTotals" to summaryTotals.take(500),
                        "driveFileId" to driveFileId.take(256),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "reportId" to cleanId,
                        "reportType" to reportType,
                        "reportTitle" to reportTitle.take(160).ifBlank { reportType },
                        "subtitle" to subtitle.take(300),
                        "businessDate" to businessDate,
                        "yearMonth" to yearMonth,
                        "generatedBy" to generatedBy.take(100).ifBlank { "Admin" },
                        "rowCount" to rowCount.coerceAtLeast(0),
                        "summaryTotals" to summaryTotals.take(500),
                        "driveFileId" to driveFileId.take(256),
                        "createdByUid" to uid,
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

    /**
     * Checks Firestore `staff_accounts` for PIN collisions before creating or updating an account PIN.
     */
    suspend fun findCloudPinCollisionDisplayName(targetUsername: String?, pin: String): String? {
        val wsId = activeWorkspaceId ?: return null
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("workspaces").document(wsId)
                .collection("staff_accounts")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            val accounts = snap.toObjects(CloudStaffAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            accounts.firstOrNull {
                it.matchesPin(pin) && (targetUsername == null || it.username != targetUsername)
            }?.let { "${it.displayName} (${it.role})" }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearLinkedStaffMetadata() {
        val uid = auth.currentUser?.uid ?: return
        _loginMetadata.value = _loginMetadata.value.copy(
            linkedStaffUsername = "",
            linkedStaffRole = null
        )
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "linkedStaffUsername" to "",
                    "linkedStaffRole" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        }
    }

    suspend fun upsertStaffAttendance(entity: StaffAttendanceEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = entity.remoteId
        val docPath = "workspaces/$wsId/staff_attendance/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("staff_attendance").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "staffName" to entity.staffName.trim().take(100).ifBlank { "Staff" },
                        "vehicleName" to entity.vehicleName.trim().take(64),
                        "dutyStartMs" to entity.dutyStartMs.coerceAtLeast(0L),
                        "lunchOutMs" to entity.lunchOutMs.coerceAtLeast(0L),
                        "lunchInMs" to entity.lunchInMs.coerceAtLeast(0L),
                        "dutyCloseMs" to entity.dutyCloseMs.coerceAtLeast(0L),
                        "updatedAtMs" to entity.updatedAtMs.coerceAtLeast(1L),
                        "updatedByUid" to uid,
                        "memberUids" to members,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "attendanceId" to docId,
                        "businessDate" to entity.businessDate,
                        "staffId" to entity.staffId.coerceAtLeast(1L),
                        "staffName" to entity.staffName.trim().take(100).ifBlank { "Staff" },
                        "vehicleName" to entity.vehicleName.trim().take(64),
                        "dutyStartMs" to entity.dutyStartMs.coerceAtLeast(0L),
                        "lunchOutMs" to entity.lunchOutMs.coerceAtLeast(0L),
                        "lunchInMs" to entity.lunchInMs.coerceAtLeast(0L),
                        "dutyCloseMs" to entity.dutyCloseMs.coerceAtLeast(0L),
                        "updatedAtMs" to entity.updatedAtMs.coerceAtLeast(1L),
                        "updatedByUid" to uid,
                        "memberUids" to members,
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

    suspend fun createPendingStoreTransfer(entity: PendingStoreTransferEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = entity.remoteId
        val docPath = "workspaces/$wsId/pending_store_transfers/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val payload = mapOf(
                "requestId" to docId,
                "businessDate" to entity.businessDate,
                "direction" to entity.direction,
                "vehicleId" to entity.vehicleId.coerceAtLeast(1L),
                "vehicleName" to entity.vehicleName.trim().take(64).ifBlank { "Vehicle" },
                "productId" to entity.productId.coerceAtLeast(1L),
                "productName" to entity.productName.trim().take(100).ifBlank { "Cylinder" },
                "productCategory" to entity.productCategory.trim().take(64),
                "quantity" to entity.quantity.coerceIn(1, 100000),
                "staffId" to entity.staffId.coerceAtLeast(1L),
                "staffName" to entity.staffName.trim().take(100).ifBlank { "Staff" },
                "notes" to entity.notes.trim().take(300),
                "status" to "PENDING",
                "requestedAtMs" to entity.requestedAtMs.coerceAtLeast(1L),
                "reviewedAtMs" to 0L,
                "reviewedBy" to "",
                "reviewNotes" to "",
                "createdByUid" to uid,
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("workspaces").document(wsId)
                .collection("pending_store_transfers").document(docId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePendingStoreTransferStatus(entity: PendingStoreTransferEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = entity.remoteId
        val docPath = "workspaces/$wsId/pending_store_transfers/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val docRef = db.collection("workspaces").document(wsId)
                .collection("pending_store_transfers").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to entity.status,
                        "reviewedAtMs" to entity.reviewedAtMs.coerceAtLeast(0L),
                        "reviewedBy" to entity.reviewedBy.trim().take(100),
                        "reviewNotes" to entity.reviewNotes.trim().take(300),
                        "memberUids" to members,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createAppNotification(entity: AppNotificationEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = entity.remoteId
        val docPath = "workspaces/$wsId/app_notifications/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val payload = mapOf(
                "notifId" to docId,
                "targetScope" to entity.targetScope,
                "targetStaffId" to entity.targetStaffId.coerceAtLeast(0L),
                "category" to entity.category,
                "title" to entity.title.trim().take(160).ifBlank { "Alert" },
                "message" to entity.message.trim().take(500).ifBlank { "Update" },
                "isUrgent" to entity.isUrgent,
                "createdAtMs" to entity.createdAtMs.coerceAtLeast(1L),
                "createdByUid" to uid,
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("workspaces").document(wsId)
                .collection("app_notifications").document(docId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createChatMessage(entity: ChatMessageEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val docId = entity.remoteId
        val docPath = "workspaces/$wsId/chat_messages/$docId"
        return try {
            val uid = requireUserId()
            val members = currentMembersIncluding(uid)
            val payload = mapOf(
                "messageId" to docId,
                "channel" to entity.channel,
                "senderId" to entity.senderId.coerceAtLeast(0L),
                "senderName" to entity.senderName.trim().take(100).ifBlank { "Team" },
                "senderRole" to entity.senderRole,
                "message" to entity.message.trim().take(1000).ifBlank { "..." },
                "isImportant" to entity.isImportant,
                "createdAtMs" to entity.createdAtMs.coerceAtLeast(1L),
                "createdByUid" to uid,
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("workspaces").document(wsId)
                .collection("chat_messages").document(docId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(
        typingId: String,
        channel: String,
        senderName: String,
        senderRole: String,
        isTyping: Boolean
    ): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        val cleanId = typingId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
        val docPath = "workspaces/$wsId/typing_status/$cleanId"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "typingId" to cleanId,
                "channel" to channel,
                "senderName" to senderName.trim().take(100).ifBlank { "Member" },
                "senderRole" to senderRole,
                "isTyping" to isTyping,
                "updatedAtMs" to System.currentTimeMillis(),
                "updatedByUid" to uid
            )
            db.collection("workspaces").document(wsId)
                .collection("typing_status").document(cleanId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushAuthorizedMobile(entity: AuthorizedMobileEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        if (_loginMetadata.value.cloudUserRole == UserRole.OWNER_VIEW) return Result.success(Unit)
        val docId = "mob_${entity.phoneE164.replace(Regex("[^0-9]"), "")}"
        val docPath = "workspaces/$wsId/authorized_mobiles/$docId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("workspaces").document(wsId)
                .collection("authorized_mobiles").document(docId)
            val existingSnap = docRef.get().await()
            val payload = linkedMapOf<String, Any?>(
                "mobileId" to docId,
                "phoneE164" to entity.phoneE164,
                "displayName" to entity.displayName.trim().take(100),
                "role" to entity.role.name,
                "staffUserId" to entity.staffUserId,
                "allowedFeaturesCsv" to entity.allowedFeaturesCsv.take(300),
                "isActive" to entity.isActive,
                "isProtectedOwner" to entity.isProtectedOwner,
                "firstTimeRegEnabled" to entity.firstTimeRegEnabled,
                "isRegistered" to entity.isRegistered,
                "otpVerifiedAtMs" to entity.otpVerifiedAtMs,
                "registeredDeviceId" to entity.registeredDeviceId.take(120),
                "createdAtMs" to entity.createdAtMs.coerceAtLeast(1L),
                "updatedAtMs" to entity.updatedAtMs.coerceAtLeast(1L),
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            if (existingSnap.exists()) {
                docRef.update(payload).await()
            } else {
                val createMap = payload.toMutableMap().apply {
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAuthorizedMobile(phoneE164: String): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        if (_loginMetadata.value.cloudUserRole == UserRole.OWNER_VIEW) return Result.success(Unit)
        val docId = "mob_${phoneE164.replace(Regex("[^0-9]"), "")}"
        val docPath = "workspaces/$wsId/authorized_mobiles/$docId"
        return try {
            requireUserId()
            db.collection("workspaces").document(wsId)
                .collection("authorized_mobiles").document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushRegisteredDevice(entity: RegisteredDeviceEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        if (_loginMetadata.value.cloudUserRole == UserRole.OWNER_VIEW) return Result.success(Unit)
        val docId = entity.deviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
        val docPath = "workspaces/$wsId/registered_devices/$docId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("workspaces").document(wsId)
                .collection("registered_devices").document(docId)
            val existingSnap = docRef.get().await()
            val payload = mapOf(
                "deviceId" to docId,
                "deviceLabel" to entity.deviceLabel.trim().take(120).ifBlank { "Android Device" },
                "phoneE164" to entity.phoneE164.trim().take(30),
                "userId" to entity.userId.coerceAtLeast(0L),
                "displayName" to entity.displayName.trim().take(100).ifBlank { "User" },
                "role" to entity.role.name,
                "sessionVersion" to entity.sessionVersion.coerceAtLeast(1L),
                "isRevoked" to entity.isRevoked,
                "lastActiveMs" to entity.lastActiveMs.coerceAtLeast(1L),
                "registeredAtMs" to entity.registeredAtMs.coerceAtLeast(1L),
                "ownerUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                docRef.update(payload).await()
            } else {
                val createMap = payload.toMutableMap().apply {
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushSecurityAuditLog(entity: SecurityAuditLogEntity): Result<Unit> {
        val wsId = activeWorkspaceId ?: return Result.success(Unit)
        if (_loginMetadata.value.cloudUserRole == UserRole.OWNER_VIEW) return Result.success(Unit)
        val docId = generateRemoteId("sec")
        val docPath = "workspaces/$wsId/security_logs/$docId"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "logId" to docId,
                "eventType" to entity.eventType.trim().take(60).ifBlank { "SECURITY_EVENT" },
                "role" to entity.role.trim().take(30).ifBlank { "SYSTEM" },
                "actorName" to entity.actorName.trim().take(100).ifBlank { "System" },
                "phoneE164" to entity.phoneE164.trim().take(30),
                "deviceLabel" to entity.deviceLabel.trim().take(120),
                "details" to entity.details.trim().take(300),
                "isAlert" to entity.isAlert,
                "timestampMs" to entity.timestampMs.coerceAtLeast(1L),
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("workspaces").document(wsId)
                .collection("security_logs").document(docId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun stopSync() {
        activeSyncJob?.cancel()
        activeSyncJob = null
        activeWorkspaceId = null
        activeMemberUids = emptyList()
        _loginMetadata.value = CloudLoginMetadata()
        _adminSettings.value = null
        _adminReports.value = emptyList()
        _staffLoginEvents.value = emptyList()
        _typingStatuses.value = emptyList()
        _connectionState.value = WorkspaceConnectionState.Checking
    }

    private fun currentMembersIncluding(uid: String): List<String> =
        (activeMemberUids + uid).distinct().take(20)

    companion object {
        const val ADMIN_EMAIL = "atozgas26@gmail.com"
        const val DEFAULT_COMPANY_WORKSPACE_ID = "concord_atoz_gas"
        const val DEFAULT_INVITE_CODE = "CONCORD2026"
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

        fun generateRemoteTxId(): String =
            "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"

        fun generateRemoteId(prefix: String): String =
            "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
    }
}
