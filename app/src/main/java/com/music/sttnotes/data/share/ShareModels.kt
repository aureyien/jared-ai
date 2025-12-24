package com.music.sttnotes.data.share

import kotlinx.serialization.Serializable

@Serializable
data class ShareRequest(
    val title: String,
    val content: String,
    val articleId: String? = null,
    val expiresInDays: Int = 7,
    val burnAfterRead: Boolean = true
)

@Serializable
data class ShareResponse(
    val id: String,
    val articleId: String?,
    val title: String,
    val content: String,
    val shortCode: String,
    val qrCodeUrl: String, // data:image/svg+xml;base64,... (ignored, we generate locally)
    val shareUrl: String,
    val viewCount: Int,
    val createdAt: String,
    val expiresAt: String,
    val lastViewedAt: String?,
    val isExpired: Boolean,
    val burnAfterRead: Boolean
)
