package com.music.sttnotes.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    content: @Composable RowScope.() -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled,
            shape = shape,
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
            shape = shape,
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EInkIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    if (onLongClick != null) {
        // Use Box with combinedClickable for long-press support
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = contentDescription
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
                tint = if (enabled) EInkBlack else EInkBlack.copy(alpha = 0.4f)
            )
        }
    } else {
        // Original IconButton for simple click
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
@OptIn(ExperimentalFoundationApi::class)
fun EInkChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val chipModifier = if (onLongClick != null) {
        modifier
            .height(40.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    } else {
        modifier.height(40.dp)
    }

    FilterChip(
        selected = selected,
        onClick = if (onLongClick != null) { {} } else onClick, // Disable FilterChip onClick if using combinedClickable
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = chipModifier,
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
    showClearButton: Boolean = false,
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
        trailingIcon = if (showClearButton && value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = EInkGrayMedium,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            trailingIcon
        },
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
 * E-Ink loading indicator - subtle sequential dot animation (e-ink friendly)
 * Each dot lights up in sequence, creating a gentle "wave" effect
 */
@Composable
fun EInkLoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "Loading..."
) {
    // Which dot is currently "active" (0, 1, or 2)
    var activeDot by remember { mutableStateOf(0) }

    // Slow animation: each dot stays active for 800ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            activeDot = (activeDot + 1) % 3
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated dots - active dot is larger, all vertically centered
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == activeDot) 10.dp else 6.dp)
                        .background(
                            if (index == activeDot) EInkBlack else EInkGrayMedium,
                            CircleShape
                        )
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
 * E-Ink bottom action bar - 2 or 3 buttons depending on showChat
 * Knowledge: open knowledge base
 * Chat button: tap = open chat list, long press = open chat with recording started (only if showChat = true)
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
    modifier: Modifier = Modifier,
    showChat: Boolean = true
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
            // Shape for left button - rounded bottom-left to match Palma 2 Pro screen corner
            val leftButtonShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 24.dp,
                bottomEnd = 0.dp
            )
            // Shape for middle button - no rounded corners
            val middleButtonShape = RoundedCornerShape(0.dp)
            // Shape for right button - rounded bottom-right to match Palma 2 Pro screen corner
            val rightButtonShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 0.dp,
                bottomEnd = 24.dp
            )

            // Knowledge Base button - BLACK background (filled) - LEFT position
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(onClick = onKnowledgeBase),
                shape = leftButtonShape,
                color = EInkBlack
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp), tint = EInkWhite)
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Knowledge", color = EInkWhite, style = MaterialTheme.typography.labelMedium)
                        Text("Base", color = EInkWhite, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            // Chat button with long press support - only show if showChat is true - WHITE background (outlined) - MIDDLE position
            if (showChat) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .combinedClickable(
                            onClick = onChat,
                            onLongClick = onChatLongPress
                        ),
                    shape = middleButtonShape,
                    color = EInkWhite,
                    border = BorderStroke(2.dp, EInkBlack)
                ) {
                    // Icon on left edge, text centered in remaining space (icon width = 20dp + 12dp padding)
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Icon at start
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(20.dp)
                                .align(Alignment.CenterStart),
                            tint = EInkBlack
                        )
                        // Text centered in the space after the icon (offset by icon area / 2)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 32.dp), // icon (20dp) + padding (12dp) = 32dp offset
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("New", color = EInkBlack, style = MaterialTheme.typography.labelMedium)
                            Text("Chat", color = EInkBlack, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Notes button with long press for recording - WHITE background (outlined) - RIGHT position
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(
                        onClick = onAddNote,
                        onLongClick = onAddNoteWithRecording
                    ),
                shape = rightButtonShape,
                color = EInkWhite,
                border = BorderStroke(2.dp, EInkBlack)
            ) {
                // Icon on left edge, text centered in remaining space
                Box(modifier = Modifier.fillMaxSize()) {
                    // Icon at start
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(20.dp)
                            .align(Alignment.CenterStart),
                        tint = EInkBlack
                    )
                    // Text centered in the space after the icon
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp), // icon (20dp) + padding (12dp) = 32dp offset
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("New", color = EInkBlack, style = MaterialTheme.typography.labelMedium)
                        Text("Note", color = EInkBlack, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/**
 * Bottom action bar for Knowledge Base screen - similar to home but with Home button instead of KB
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EInkKBBottomActionBar(
    onHome: () -> Unit,
    onChat: () -> Unit,
    onChatLongPress: () -> Unit,
    onAddNote: () -> Unit,
    onAddNoteWithRecording: () -> Unit,
    modifier: Modifier = Modifier,
    showChat: Boolean = true
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
            // Shape for left button - rounded bottom-left to match Palma 2 Pro screen corner
            val leftButtonShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 24.dp,
                bottomEnd = 0.dp
            )
            // Shape for middle button - no rounded corners
            val middleButtonShape = RoundedCornerShape(0.dp)
            // Shape for right button - rounded bottom-right to match Palma 2 Pro screen corner
            val rightButtonShape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 0.dp,
                bottomEnd = 24.dp
            )

            // Home button - BLACK background (filled) - LEFT position
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(onClick = onHome),
                shape = leftButtonShape,
                color = EInkBlack
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp), tint = EInkWhite)
                    Spacer(Modifier.width(6.dp))
                    Text("Home", color = EInkWhite, style = MaterialTheme.typography.labelLarge)
                }
            }

            // Chat button - only show if showChat is true - MIDDLE position
            if (showChat) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .combinedClickable(
                            onClick = onChat,
                            onLongClick = onChatLongPress
                        ),
                    shape = middleButtonShape,
                    color = EInkWhite,
                    border = BorderStroke(2.dp, EInkBlack)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(20.dp)
                                .align(Alignment.CenterStart),
                            tint = EInkBlack
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("New", color = EInkBlack, style = MaterialTheme.typography.labelSmall)
                            Text("Chat", color = EInkBlack, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Notes button - RIGHT position
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .combinedClickable(
                        onClick = onAddNote,
                        onLongClick = onAddNoteWithRecording
                    ),
                shape = rightButtonShape,
                color = EInkWhite,
                border = BorderStroke(2.dp, EInkBlack)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(20.dp)
                            .align(Alignment.CenterStart),
                        tint = EInkBlack
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("New", color = EInkBlack, style = MaterialTheme.typography.labelSmall)
                        Text("Note", color = EInkBlack, style = MaterialTheme.typography.labelLarge)
                    }
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
 * Compact UNDO button with 5 dots countdown (e-ink optimized)
 * Designed to be placed in TopAppBar actions
 * Shows 5 dots that disappear one by one each second
 */
@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    durationMs: Long = 5000L,
    modifier: Modifier = Modifier
) {
    UndoButton(
        onUndo = onUndo,
        onTimeout = onTimeout,
        modifier = modifier
    )
}

/**
 * Compact UNDO button with 5 dots countdown for TopAppBar
 * Shows 5 dots on top of "UNDO" text, one dot disappears each second
 * @param itemKey unique key to restart countdown when deleting a different item
 */
@Composable
fun UndoButton(
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    itemKey: Any? = null
) {
    // Use key() to force complete recomposition when itemKey changes
    // This ensures the old LaunchedEffect is fully cancelled before new one starts
    key(itemKey) {
        UndoButtonContent(
            onUndo = onUndo,
            onTimeout = onTimeout,
            modifier = modifier
        )
    }
}

@Composable
private fun UndoButtonContent(
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 dots countdown - starts fresh each time this composable is created
    var dotsRemaining by remember { mutableIntStateOf(5) }

    // Countdown once per second
    LaunchedEffect(Unit) {
        while (dotsRemaining > 0) {
            delay(1000)
            dotsRemaining--
        }
        onTimeout()
    }

    Surface(
        modifier = modifier,
        color = EInkBlack,
        shape = RoundedCornerShape(4.dp),
        onClick = onUndo
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 5 dots row - disappear from left to right
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(5) { index ->
                    // Dots disappear left to right: index 0 disappears first when dotsRemaining=4
                    val dotsGone = 5 - dotsRemaining
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (index >= dotsGone) EInkWhite else EInkBlack,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "UNDO",
                color = EInkWhite,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * E-Ink optimized modal overlay - solid white background, black border, no shadows
 *
 * Usage:
 * var showModal by remember { mutableStateOf(false) }
 * if (showModal) {
 *     EInkModal(
 *         onDismiss = { showModal = false },
 *         title = "Confirmation",
 *         buttons = {
 *             EInkButton(onClick = { showModal = false }) { Text("OK") }
 *         }
 *     ) {
 *         Text("Modal content")
 *     }
 * }
 */
@Composable
fun EInkModal(
    onDismiss: () -> Unit,
    title: String? = null,
    buttons: @Composable RowScope.() -> Unit = {},
    dismissOnBackgroundClick: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // Handle back button
    BackHandler(onBack = onDismiss)

    // Full-screen overlay with solid white background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EInkWhite)
            .clickable(
                onClick = if (dismissOnBackgroundClick) onDismiss else {{}},
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Modal content card with border
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
                .clickable(
                    onClick = {}, // Prevent clicks from passing through
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .border(
                    width = 2.dp,
                    color = EInkBlack,
                    shape = RoundedCornerShape(8.dp)
                ),
            color = EInkWhite,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Title
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        color = EInkBlack
                    )
                    Spacer(Modifier.height(16.dp))
                    EInkDivider()
                    Spacer(Modifier.height(16.dp))
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    content = content
                )

                // Buttons (if provided)
                if (buttons != {}) {
                    Spacer(Modifier.height(20.dp))
                    EInkDivider()
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = buttons
                    )
                }
            }
        }
    }
}

/**
 * E-Ink optimized confirmation modal - common pattern for yes/no actions
 */
@Composable
fun EInkConfirmationModal(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    destructive: Boolean = false
) {
    EInkModal(
        onDismiss = onDismiss,
        title = title,
        buttons = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text(dismissText)
            }
            Spacer(Modifier.width(12.dp))
            EInkButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                filled = true
            ) {
                Text(confirmText)
            }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = EInkBlack
        )
    }
}

/**
 * E-Ink optimized form modal - for text input dialogs
 */
@Composable
fun EInkFormModal(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    confirmText: String = "Save",
    dismissText: String = "Cancel",
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    EInkModal(
        onDismiss = onDismiss,
        title = title,
        buttons = {
            EInkButton(
                onClick = onDismiss,
                filled = false
            ) {
                Text(dismissText)
            }
            Spacer(Modifier.width(12.dp))
            EInkButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                filled = true,
                enabled = confirmEnabled
            ) {
                Text(confirmText)
            }
        },
        content = content
    )
}
