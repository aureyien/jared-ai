package com.music.sttnotes.data.share

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareService @Inject constructor() {
    private val okHttpClient = OkHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val BASE_URL = "https://readtoken.app/api"
    }

    suspend fun createShare(
        title: String,
        content: String,
        articleId: String? = null,
        expiresInDays: Int = 7,
        apiToken: String
    ): Result<ShareResponse> = withContext(Dispatchers.IO) {
        try {
            val shareRequest = ShareRequest(
                title = title,
                content = content,
                articleId = articleId,
                expiresInDays = expiresInDays
            )

            val requestBody = json.encodeToString(
                ShareRequest.serializer(),
                shareRequest
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/share")
                .addHeader("Authorization", "Bearer $apiToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val shareResponse = json.decodeFromString<ShareResponse>(responseBody)
                Result.success(shareResponse)
            } else {
                Result.failure(
                    Exception("Share API error: ${response.code} ${response.message}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
