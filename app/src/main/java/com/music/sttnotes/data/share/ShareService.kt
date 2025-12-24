package com.music.sttnotes.data.share

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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
        private const val BASE_URL = "https://api.readtoken.app/api"
    }

    /**
     * Generate QR code bitmap from URL
     * @param content URL to encode in QR code
     * @param size Size of the QR code in pixels (default 512)
     * @return Bitmap of the QR code
     */
    fun generateQrCode(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    suspend fun createShare(
        title: String,
        content: String,
        articleId: String? = null,
        expiresInDays: Int = 7,
        burnAfterRead: Boolean = true,
        apiToken: String
    ): Result<ShareResponse> = withContext(Dispatchers.IO) {
        try {
            val shareRequest = ShareRequest(
                title = title,
                content = content,
                articleId = articleId,
                expiresInDays = expiresInDays,
                burnAfterRead = burnAfterRead
            )

            val requestBody = json.encodeToString(
                ShareRequest.serializer(),
                shareRequest
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/shares")
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
