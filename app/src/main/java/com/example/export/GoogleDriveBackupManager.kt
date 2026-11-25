package com.example.export

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface DriveAuthResult {
    data class Authorized(val accessToken: String) : DriveAuthResult
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthResult
    data class Error(val message: String) : DriveAuthResult
}

data class DriveUploadReceipt(
    val fileId: String,
    val fileName: String,
    val uploadedAtMs: Long
)

data class DriveBackupState(
    val autoBackupEnabled: Boolean = false,
    val driveAuthorized: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastBackupAtMs: Long = 0L,
    val lastFileId: String = "",
    val lastStatusMessage: String = "Google Drive backup ready to connect"
)

class GoogleDriveBackupManager(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    suspend fun authorizeDriveAccess(allowConsentPrompt: Boolean): DriveAuthResult {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(SCOPE_DRIVE_FILE),
                        Scope(SCOPE_DRIVE_APPDATA)
                    )
                )
                .build()
            val result = Identity.getAuthorizationClient(context.applicationContext)
                .authorize(request)
                .await()

            if (result.hasResolution()) {
                val pending = result.pendingIntent
                if (allowConsentPrompt && pending != null) {
                    DriveAuthResult.NeedsConsent(pending)
                } else {
                    DriveAuthResult.Error("Google Drive authorization needed. Tap Authorize Google Drive.")
                }
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    DriveAuthResult.Authorized(token)
                } else {
                    DriveAuthResult.Error("Google Drive access token was empty.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DriveAuthResult.Error(e.localizedMessage ?: "Unable to connect Google Drive authorization.")
        }
    }

    fun extractTokenFromConsentResult(data: Intent?): String? {
        if (data == null) return null
        return try {
            Identity.getAuthorizationClient(context.applicationContext)
                .getAuthorizationResultFromIntent(data)
                .accessToken
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadJsonBackup(
        jsonContent: String,
        fileName: String = BACKUP_FILE_NAME,
        accessTokenOverride: String? = null
    ): Result<DriveUploadReceipt> = withContext(Dispatchers.IO) {
        uploadBytesInternal(
            bytes = jsonContent.toByteArray(Charsets.UTF_8),
            fileName = fileName,
            mimeType = "application/json",
            accessTokenOverride = accessTokenOverride,
            alsoMirrorToAppData = true
        )
    }

    suspend fun uploadReportDocument(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        accessTokenOverride: String? = null
    ): Result<DriveUploadReceipt> = withContext(Dispatchers.IO) {
        uploadBytesInternal(
            bytes = bytes,
            fileName = fileName,
            mimeType = mimeType,
            accessTokenOverride = accessTokenOverride,
            alsoMirrorToAppData = false
        )
    }

    suspend fun downloadLatestBackupJson(
        accessTokenOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = accessTokenOverride ?: when (val auth = authorizeDriveAccess(allowConsentPrompt = false)) {
                is DriveAuthResult.Authorized -> auth.accessToken
                is DriveAuthResult.NeedsConsent ->
                    return@withContext Result.failure(IllegalStateException("Authorize Google Drive first."))
                is DriveAuthResult.Error ->
                    return@withContext Result.failure(IllegalStateException(auth.message))
            }

            val fileId = findExistingFileId(token, BACKUP_FILE_NAME, space = "drive")
                ?: findExistingFileId(token, BACKUP_FILE_NAME, space = "appDataFolder")
                ?: return@withContext Result.failure(
                    IllegalStateException("No '$BACKUP_FILE_NAME' backup file found in Google Drive yet.")
                )

            val downloadReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(downloadReq).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Google Drive download failed (${resp.code}): ${extractDriveError(bodyStr)}")
                    )
                }
                if (bodyStr.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Google Drive backup file is empty."))
                }
                Result.success(bodyStr)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadBytesInternal(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        accessTokenOverride: String?,
        alsoMirrorToAppData: Boolean
    ): Result<DriveUploadReceipt> {
        return try {
            val token = accessTokenOverride ?: when (val auth = authorizeDriveAccess(allowConsentPrompt = false)) {
                is DriveAuthResult.Authorized -> auth.accessToken
                is DriveAuthResult.NeedsConsent ->
                    return Result.failure(IllegalStateException("Authorize Google Drive first."))
                is DriveAuthResult.Error ->
                    return Result.failure(IllegalStateException(auth.message))
            }

            val existingDriveId = findExistingFileId(token, fileName, space = "drive")
            val uploaded = upsertDriveFile(
                token = token,
                existingFileId = existingDriveId,
                fileName = fileName,
                mimeType = mimeType,
                bytes = bytes,
                inAppDataFolder = false
            ).getOrElse { return Result.failure(it) }

            if (alsoMirrorToAppData) {
                val appDataId = findExistingFileId(token, fileName, space = "appDataFolder")
                upsertDriveFile(
                    token = token,
                    existingFileId = appDataId,
                    fileName = fileName,
                    mimeType = mimeType,
                    bytes = bytes,
                    inAppDataFolder = true
                )
            }

            Result.success(uploaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findExistingFileId(token: String, fileName: String, space: String): String? {
        val safeName = fileName.replace("'", "\\'")
        val query = URLEncoder.encode("name = '$safeName' and trashed = false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$query&spaces=$space&fields=files(id,name,modifiedTime)&pageSize=5"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string().orEmpty()
                val arr = JSONObject(raw).optJSONArray("files") ?: return null
                if (arr.length() == 0) null else arr.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun upsertDriveFile(
        token: String,
        existingFileId: String?,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        inAppDataFolder: Boolean
    ): Result<DriveUploadReceipt> {
        val metadata = JSONObject().apply {
            if (existingFileId == null) {
                put("name", fileName)
                if (inAppDataFolder) {
                    put("parents", JSONArray().put("appDataFolder"))
                }
            }
            put("description", "Concord & A to Z Gas Automatic Cloud Backup")
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addPart(bytes.toRequestBody(mimeType.toMediaType()))
            .build()

        val reqBuilder = Request.Builder()
            .header("Authorization", "Bearer $token")

        val request = if (existingFileId != null) {
            reqBuilder
                .url("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart&fields=id,name")
                .patch(multipartBody)
                .build()
        } else {
            reqBuilder
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
                .post(multipartBody)
                .build()
        }

        return httpClient.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                Result.failure(
                    IllegalStateException("Google Drive upload failed (${resp.code}): ${extractDriveError(raw)}")
                )
            } else {
                val json = try {
                    JSONObject(raw)
                } catch (_: Exception) {
                    JSONObject()
                }
                val fileId = json.optString("id").ifBlank { existingFileId.orEmpty() }
                Result.success(
                    DriveUploadReceipt(
                        fileId = fileId,
                        fileName = json.optString("name").ifBlank { fileName },
                        uploadedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun extractDriveError(raw: String): String {
        return try {
            val errObj = JSONObject(raw).optJSONObject("error")
            errObj?.optString("message")?.takeIf { it.isNotBlank() } ?: raw.take(180)
        } catch (_: Exception) {
            raw.take(180).ifBlank { "Network or authorization error" }
        }
    }

    companion object {
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_FILE_NAME = "CONCORD_ATOZ_GAS_AUTO_BACKUP.json"
    }
}
