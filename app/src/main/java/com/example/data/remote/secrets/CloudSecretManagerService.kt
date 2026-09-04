package com.example.data.remote.secrets

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for retrieving API keys and secrets securely via Google Cloud Secret Manager.
 * 
 * In production environments, client apps or backend proxies query Secret Manager directly
 * via: https://secretmanager.googleapis.com/v1/projects/{projectId}/secrets/{secretId}/versions/latest:access
 * 
 * If a Cloud Secret Manager secret resource is configured, this service fetches and decodes
 * the secret payload. Otherwise, it falls back seamlessly to BuildConfig secrets configured
 * via the AI Studio Secrets panel.
 */
object CloudSecretManagerService {
    private const val TAG = "CloudSecretManager"
    private const val SECRET_MANAGER_BASE_URL = "https://secretmanager.googleapis.com/v1"

    // In-memory cache for retrieved secrets (never written to disk or logs)
    @Volatile
    private var cachedGeminiApiKey: String? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Resolves the Gemini API Key securely.
     * 1. Checks in-memory volatile cache.
     * 2. If GCP project and Secret Name are configured, fetches from Google Cloud Secret Manager.
     * 3. Falls back to BuildConfig.GEMINI_API_KEY (injected by Secrets Gradle Plugin).
     */
    suspend fun getGeminiApiKey(
        projectId: String? = null,
        secretId: String = "gemini-api-key",
        bearerToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        // Check cache first
        cachedGeminiApiKey?.let { return@withContext it }

        // Attempt retrieval via Google Cloud Secret Manager if project and token are provided
        if (!projectId.isNullOrBlank() && !bearerToken.isNullOrBlank()) {
            try {
                val secretUrl = "$SECRET_MANAGER_BASE_URL/projects/$projectId/secrets/$secretId/versions/latest:access"
                val request = Request.Builder()
                    .url(secretUrl)
                    .addHeader("Authorization", "Bearer $bearerToken")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    val json = JSONObject(responseBody)
                    val payload = json.optJSONObject("payload")
                    val base64Data = payload?.optString("data").orEmpty()
                    if (base64Data.isNotBlank()) {
                        val decodedKey = String(Base64.decode(base64Data, Base64.DEFAULT)).trim()
                        if (decodedKey.isNotBlank()) {
                            Log.i(TAG, "Successfully retrieved secret '$secretId' via Google Cloud Secret Manager.")
                            cachedGeminiApiKey = decodedKey
                            return@withContext decodedKey
                        }
                    }
                } else {
                    Log.w(TAG, "Google Cloud Secret Manager returned HTTP ${response.code}: falling back to BuildConfig.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch secret from Google Cloud Secret Manager (${e.message}), using fallback.", e)
            }
        }

        // Fallback to BuildConfig injected via Secrets Gradle Plugin (.env / AI Studio Secrets panel)
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            cachedGeminiApiKey = buildConfigKey
            return@withContext buildConfigKey
        }

        buildConfigKey
    }

    /**
     * Clear cached secrets on user logout or session reset.
     */
    fun clearCache() {
        cachedGeminiApiKey = null
    }
}
