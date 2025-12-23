package com.music.sttnotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkWhite

/**
 * Shared Markdown formatting toolbar for RichTextEditor
 * Used in both NoteEditorScreen and KnowledgeBaseDetailScreen
 */
@Composable
fun MarkdownToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    // Detect current styles
    val currentStyle = richTextState.currentSpanStyle
    val isHeader = currentStyle.fontSize == 24.sp
    // Bold is active only if fontWeight is Bold AND it's not a header
    val isBold = currentStyle.fontWeight == FontWeight.Bold && currentStyle.fontSize != 24.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = EInkWhite,
        border = BorderStroke(1.dp, EInkBlack)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            item {
                ToolbarButton(
                    icon = Icons.Default.Title,
                    contentDescription = "Header",
                    isActive = isHeader,
                    onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                )
            }
            // Bold
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    isActive = isBold,
                    onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                )
            }
            // Italic
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    isActive = currentStyle.fontStyle == FontStyle.Italic,
                    onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    }
                )
            }
            // Underline
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline",
                    isActive = currentStyle.textDecoration == TextDecoration.Underline,
                    onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    }
                )
            }
            // Strikethrough
            item {
                ToolbarButton(
                    icon = Icons.Default.StrikethroughS,
                    contentDescription = "Strikethrough",
                    isActive = currentStyle.textDecoration == TextDecoration.LineThrough,
                    onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    }
                )
            }
            // Code
            item {
                ToolbarButton(
                    icon = Icons.Default.Code,
                    contentDescription = "Code",
                    isActive = richTextState.isCodeSpan,
                    onClick = { richTextState.toggleCodeSpan() }
                )
            }
            // Bullet List
            item {
                ToolbarButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet list",
                    isActive = richTextState.isUnorderedList,
                    onClick = { richTextState.toggleUnorderedList() }
                )
            }
            // Numbered List
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    isActive = richTextState.isOrderedList,
                    onClick = { richTextState.toggleOrderedList() }
                )
            }
            // Checkbox / Todo item
            item {
                ToolbarButton(
                    icon = Icons.Default.CheckBox,
                    contentDescription = "Checkbox",
                    isActive = false,
                    onClick = {
                        richTextState.addTextAfterSelection("- [ ] ")
                    }
                )
            }
            // Horizontal separator
            item {
                ToolbarButton(
                    icon = Icons.Default.HorizontalRule,
                    contentDescription = "Separator",
                    isActive = false,
                    onClick = {
                        richTextState.addTextAfterSelection("\n\n---\n\n")
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                if (isActive) EInkBlack else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) EInkWhite else EInkBlack
        )
    }
}
