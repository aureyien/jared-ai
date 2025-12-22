package com.music.sttnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.music.sttnotes.data.i18n.rememberStrings
import com.music.sttnotes.data.share.ShareResponse
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShareResultModal(
    shareResponse: ShareResponse,
    onDismiss: () -> Unit
) {
    val strings = rememberStrings()
    val clipboardManager = LocalClipboardManager.current

    EInkModal(
        onDismiss = onDismiss,
        title = strings.shareArticle,
        buttons = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text(strings.close)
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QR Code
            AsyncImage(
                model = shareResponse.qrCodeUrl,
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(200.dp)
                    .background(EInkWhite, RoundedCornerShape(8.dp))
            )

            // Share URL with copy button
            EInkCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.shareUrl,
                        style = MaterialTheme.typography.labelMedium,
                        color = EInkGrayMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shareResponse.shareUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = EInkBlack,
                            modifier = Modifier.weight(1f)
                        )
                        EInkIconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(shareResponse.shareUrl))
                            },
                            icon = Icons.Default.ContentCopy,
                            contentDescription = strings.copy
                        )
                    }
                }
            }

            // Expiration info
            Text(
                text = "${strings.expiresOn}: ${formatDate(shareResponse.expiresAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = EInkGrayMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoDate)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        isoDate
    }
}
