package com.example.data.remote

import android.content.Context
import androidx.room.withTransaction
import com.example.R
import com.example.data.local.ConcordDatabase
import com.example.data.local.OpeningStockEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.UserAccountEntity
import com.example.data.local.VehicleEntity
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
                "companyName" to "CONCORD & A TO Z GAS",
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
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to displayName.take(100),
                "email" to email.take(200),
                "activeWorkspaceId" to "",
                "linkedStaffUsername" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        } else {
            ref.update(
                mapOf(
                    "displayName" to displayName.take(100),
                    "email" to email.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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
                            val isOwnerOrAdmin = ws.ownerUid == uid || isAdminEmail
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
                                        if (existing.pin != cu.pin || existing.displayName != cu.displayName || existing.isActive != cu.active) {
                                            localDb.userDao().insert(
                                                existing.copy(
                                                    displayName = cu.displayName,
                                                    pin = cu.pin,
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
                                        val updated = match.copy(
                                            name = cp.name,
                                            sortOrder = cp.sortOrder,
                                            isActive = cp.active
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
                            val existingRemoteIds = existingTxs.mapNotNull { it.remoteId }.toSet()

                            for (ct in cloudTxs) {
                                val entity = ct.toEntity() ?: continue
                                val slug = entity.remoteId ?: continue
                                if (slug !in existingRemoteIds) {
                                    localDb.transactionDao().insertIgnore(entity)
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
                        "name" to product.name.take(100),
                        "sortOrder" to product.sortOrder,
                        "active" to product.isActive,
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
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "pin" to user.pin,
                        "displayName" to user.displayName.take(100),
                        "active" to user.isActive,
                        "memberUids" to members,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(CloudStaffAccount.toCreateMap(user, members, uid)).await()
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

    fun stopSync() {
        activeSyncJob?.cancel()
        activeSyncJob = null
        activeWorkspaceId = null
        activeMemberUids = emptyList()
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
    }
}
