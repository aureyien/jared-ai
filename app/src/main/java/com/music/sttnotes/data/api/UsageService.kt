package com.music.sttnotes.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class UsageStats(
    val cost: Double,
    val inputTokens: Long,
    val outputTokens: Long
) {
    val totalTokens: Long get() = inputTokens + outputTokens
}

@Singleton
class UsageService @Inject constructor() {
    private val client = OkHttpClient.Builder().build()

    /**
     * Fetch OpenAI costs and tokens for the last N days
     * Uses two APIs: Costs for billing data, Usage for token counts
     * Returns total cost in USD and total tokens
     */
    suspend fun fetchOpenAIUsage(apiKey: String, days: Int = 30): Result<UsageStats> = withContext(Dispatchers.IO) {
        try {
            Log.d("UsageService", "fetchOpenAIUsage called for $days days")
            val startTime = Instant.now().minusSeconds(days * 86400L).epochSecond

            // First fetch costs from Costs API
            val costsUrl = "https://api.openai.com/v1/organization/costs?start_time=$startTime&bucket_width=1d&limit=$days"
            Log.d("UsageService", "Costs URL: $costsUrl")
            Log.d("UsageService", "Using API key: ${apiKey.take(10)}... (${apiKey.length} chars)")
            val costsRequest = Request.Builder()
                .url(costsUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            Log.d("UsageService", "Calling Costs API...")
            val costsResponse = client.newCall(costsRequest).execute()
            val costsBody = costsResponse.body?.string()
            Log.d("UsageService", "Costs response code: ${costsResponse.code}")

            if (costsBody == null) {
                Log.e("UsageService", "Empty costs response body")
                return@withContext Result.failure(Exception("Empty costs response"))
            }

            if (!costsResponse.isSuccessful) {
                Log.e("UsageService", "Costs API error: ${costsResponse.code}, body: $costsBody")
                return@withContext Result.failure(Exception("Costs API error: ${costsResponse.code}"))
            }

            val costsJson = JSONObject(costsBody)
            val data = costsJson.getJSONArray("data")
            var totalCost = 0.0

            Log.d("UsageService", "Processing ${data.length()} cost buckets")
            for (i in 0 until data.length()) {
                val bucket = data.getJSONObject(i)
                if (bucket.has("results")) {
                    val results = bucket.getJSONArray("results")
                    for (j in 0 until results.length()) {
                        val result = results.getJSONObject(j)
                        if (result.has("amount")) {
                            val amount = result.getJSONObject("amount")
                            val value = amount.getString("value").toDouble()
                            Log.d("UsageService", "Adding cost: $value")
                            totalCost += value
                        }
                    }
                }
            }
            Log.d("UsageService", "Total cost: $totalCost")

            // Then fetch tokens from Usage API (completions endpoint)
            val usageRequest = Request.Builder()
                .url("https://api.openai.com/v1/organization/usage/completions?start_time=$startTime&bucket_width=1d&limit=$days")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val usageResponse = client.newCall(usageRequest).execute()
            val usageBody = usageResponse.body?.string()

            var totalInputTokens = 0L
            var totalOutputTokens = 0L
            if (usageResponse.isSuccessful && usageBody != null) {
                Log.d("UsageService", "OpenAI Usage API response (first 500 chars): ${usageBody.take(500)}")
                val usageJson = JSONObject(usageBody)
                if (usageJson.has("data")) {
                    val buckets = usageJson.getJSONArray("data")
                    Log.d("UsageService", "Found ${buckets.length()} usage buckets")
                    for (i in 0 until buckets.length()) {
                        val bucket = buckets.getJSONObject(i)
                        if (bucket.has("results")) {
                            val results = bucket.getJSONArray("results")
                            for (j in 0 until results.length()) {
                                val result = results.getJSONObject(j)
                                // Track input and output separately
                                if (result.has("input_tokens")) {
                                    val inputTokens = result.getLong("input_tokens")
                                    Log.d("UsageService", "Bucket $i Result $j: input_tokens=$inputTokens")
                                    totalInputTokens += inputTokens
                                }
                                if (result.has("output_tokens")) {
                                    val outputTokens = result.getLong("output_tokens")
                                    Log.d("UsageService", "Bucket $i Result $j: output_tokens=$outputTokens")
                                    totalOutputTokens += outputTokens
                                }
                            }
                        }
                    }
                }
                Log.d("UsageService", "Total input tokens: $totalInputTokens, output tokens: $totalOutputTokens, total: ${totalInputTokens + totalOutputTokens}")
            } else {
                Log.e("UsageService", "Usage API failed: ${usageResponse.code}, body: $usageBody")
            }

            Result.success(UsageStats(totalCost, totalInputTokens, totalOutputTokens))
        } catch (e: Exception) {
            Log.e("UsageService", "Error fetching OpenAI usage", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch Anthropic costs and tokens for the last N days
     * Requires Admin API key (sk-ant-admin...)
     * Returns total cost in USD and total tokens
     */
    suspend fun fetchAnthropicUsage(adminApiKey: String, days: Int = 30): Result<UsageStats> = withContext(Dispatchers.IO) {
        try {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(days.toLong())
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/organizations/cost_report?start_date=${startDate.format(formatter)}&end_date=${endDate.format(formatter)}")
                .addHeader("x-api-key", adminApiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API error: ${response.code}"))
            }

            val json = JSONObject(body)
            val dailyCosts = json.getJSONArray("daily_costs")
            var totalCost = 0.0
            var totalInputTokens = 0L
            var totalOutputTokens = 0L

            for (i in 0 until dailyCosts.length()) {
                val day = dailyCosts.getJSONObject(i)
                totalCost += day.getString("amount").toDouble() // Already in dollars (decimal string)

                // Track input and output separately
                if (day.has("input_tokens")) {
                    totalInputTokens += day.getLong("input_tokens")
                }
                if (day.has("output_tokens")) {
                    totalOutputTokens += day.getLong("output_tokens")
                }
            }

            Result.success(UsageStats(totalCost, totalInputTokens, totalOutputTokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
