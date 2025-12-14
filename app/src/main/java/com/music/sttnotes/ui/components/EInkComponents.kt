package com.music.sttnotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayMedium
import com.music.sttnotes.ui.theme.EInkWhite

/**
 * E-Ink optimized button - no elevation, clear border
 */
@Composable
fun EInkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EInkBlack,
                contentColor = EInkWhite,
                disabledContainerColor = EInkGrayLight,
                disabledContentColor = EInkBlack.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EInkBlack
            ),
            border = BorderStroke(2.dp, if (enabled) EInkBlack else EInkGrayLight),
            content = content
        )
    }
}

/**
 * E-Ink optimized icon button - larger touch target
 */
@Composable
fun EInkIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            tint = if (enabled) EInkBlack else EInkBlack.copy(alpha = 0.4f)
        )
    }
}

/**
 * E-Ink optimized card - no elevation, sharp border
 */
@Composable
fun EInkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, EInkBlack),
            colors = CardDefaults.outlinedCardColors(
                containerColor = EInkWhite
            ),
            content = content
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, EInkBlack),
            colors = CardDefaults.outlinedCardColors(
                containerColor = EInkWhite
            ),
            content = content
        )
    }
}

/**
 * E-Ink optimized chip - high contrast border
 */
@Composable
fun EInkChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(4.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = EInkWhite,
            labelColor = EInkBlack,
            selectedContainerColor = EInkBlack,
            selectedLabelColor = EInkWhite
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = EInkBlack,
            selectedBorderColor = EInkBlack,
            borderWidth = if (selected) 2.dp else 1.dp,
            selectedBorderWidth = 2.dp,
            enabled = true,
            selected = selected
        )
    )
}

/**
 * E-Ink optimized text field - clear border, no animations
 */
@Composable
fun EInkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                color = EInkGrayMedium,
                style = textStyle
            )
        },
        singleLine = singleLine,
        readOnly = readOnly,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(4.dp),
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EInkBlack,
            unfocusedBorderColor = EInkGrayMedium,
            focusedContainerColor = EInkWhite,
            unfocusedContainerColor = EInkWhite,
            cursorColor = EInkBlack
        )
    )
}

/**
 * E-Ink divider - sharp line
 */
@Composable
fun EInkDivider(
    modifier: Modifier = Modifier,
    thickness: Float = 1f
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness.dp,
        color = EInkBlack
    )
}

/**
 * E-Ink loading indicator - static dots instead of spinning
 */
@Composable
fun EInkLoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "Loading..."
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Static dots instead of animation
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(EInkBlack, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * E-Ink bottom action bar - 3 buttons
 * Knowledge: open knowledge base
 * Chat button: tap = open chat list, long press = open chat with recording started
 * New Note button: tap = new note, long press = new note with recording
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EInkBottomActionBar(
    onAddNote: () -> Unit,
    onAddNoteWithRecording: () -> Unit,
    onChat: () -> Unit,
    onChatLongPress: () -> Unit,
    onKnowledgeBase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = EInkWhite,
        border = BorderStroke(1.dp, EInkBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Knowledge Base button
            OutlinedButton(
                onClick = onKnowledgeBase,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EInkBlack
                ),
                border = BorderStroke(2.dp, EInkBlack)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("KB")
            }
            Spacer(Modifier.width(8.dp))
            // Chat button with long press support
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(
                        onClick = onChat,
                        onLongClick = onChatLongPress
                    ),
                shape = RoundedCornerShape(4.dp),
                color = EInkWhite,
                border = BorderStroke(2.dp, EInkBlack)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp), tint = EInkBlack)
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", color = EInkBlack)
                }
            }
            Spacer(Modifier.width(8.dp))
            // New Note button with long press for recording
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(
                        onClick = onAddNote,
                        onLongClick = onAddNoteWithRecording
                    ),
                shape = RoundedCornerShape(4.dp),
                color = EInkBlack
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = EInkWhite)
                    Spacer(Modifier.width(4.dp))
                    Text("Note", color = EInkWhite)
                }
            }
        }
    }
}

/**
 * E-Ink recording button - static visual with border, no pulse animation
 */
@Composable
fun EInkRecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(if (isRecording) 72.dp else 64.dp)
            .then(
                if (isRecording) {
                    Modifier.border(3.dp, EInkBlack, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) EInkBlack else EInkWhite,
                contentColor = if (isRecording) EInkWhite else EInkBlack
            ),
            border = if (!isRecording) BorderStroke(2.dp, EInkBlack) else null,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Data class to hold pending deletion info
 */
data class PendingDeletion<T>(
    val item: T,
    val message: String
)

/**
 * Undo Snackbar with progress bar that counts down 7 seconds
 * Shows at the bottom of the screen with UNDO button
 * Progress bar animates from left to right on top of the snackbar
 */
@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    durationMs: Long = 7000L,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }

    // Animate progress and trigger timeout - use message as key to restart if shown again
    LaunchedEffect(message) {
        val startTime = System.currentTimeMillis()
        while (progress < 1f) {
            delay(50)
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
        }
        onTimeout()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = EInkBlack,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            // Progress bar on top
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = EInkWhite,
                trackColor = EInkGrayMedium
            )

            // Content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message,
                    color = EInkWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = EInkWhite
                    )
                ) {
                    Text(
                        text = "UNDO",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
