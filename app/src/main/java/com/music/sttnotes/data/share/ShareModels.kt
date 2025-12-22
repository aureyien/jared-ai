package com.music.sttnotes.data.share

import kotlinx.serialization.Serializable

@Serializable
data class ShareRequest(
    val title: String,
    val content: String,
    val articleId: String? = null,
    val expiresInDays: Int = 7
)

@Serializable
data class ShareResponse(
    val id: String,
    val title: String,
    val shortCode: String,
    val qrCodeUrl: String, // data:image/png;base64,...
    val shareUrl: String,
    val viewCount: Int,
    val createdAt: String,
    val expiresAt: String,
    val isExpired: Boolean
)
