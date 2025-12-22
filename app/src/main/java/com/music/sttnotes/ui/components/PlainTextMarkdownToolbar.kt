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
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkWhite

/**
 * Markdown formatting toolbar for plain TextField
 * Inserts markdown syntax at cursor position
 */
@Composable
fun PlainTextMarkdownToolbar(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = EInkWhite,
        border = BorderStroke(1.dp, EInkBlack)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Header
            item {
                ToolbarButton(
                    icon = Icons.Default.Title,
                    contentDescription = "Header",
                    onClick = {
                        insertMarkdown(textFieldValue, onTextChange, "## ", "")
                    }
                )
            }
            // Bold
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    onClick = {
                        wrapSelection(textFieldValue, onTextChange, "**", "**")
                    }
                )
            }
            // Italic
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    onClick = {
                        wrapSelection(textFieldValue, onTextChange, "*", "*")
                    }
                )
            }
            // Underline (not standard markdown, but we can use HTML)
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline",
                    onClick = {
                        wrapSelection(textFieldValue, onTextChange, "<u>", "</u>")
                    }
                )
            }
            // Strikethrough
            item {
                ToolbarButton(
                    icon = Icons.Default.StrikethroughS,
                    contentDescription = "Strikethrough",
                    onClick = {
                        wrapSelection(textFieldValue, onTextChange, "~~", "~~")
                    }
                )
            }
            // Code
            item {
                ToolbarButton(
                    icon = Icons.Default.Code,
                    contentDescription = "Code",
                    onClick = {
                        wrapSelection(textFieldValue, onTextChange, "`", "`")
                    }
                )
            }
            // Bullet List
            item {
                ToolbarButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet list",
                    onClick = {
                        insertMarkdown(textFieldValue, onTextChange, "- ", "")
                    }
                )
            }
            // Numbered List
            item {
                ToolbarButton(
                    icon = Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    onClick = {
                        insertMarkdown(textFieldValue, onTextChange, "1. ", "")
                    }
                )
            }
            // Empty checkbox - with smart toggle
            item {
                ToolbarButton(
                    icon = Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Empty checkbox",
                    onClick = {
                        toggleCheckboxOnCurrentLine(textFieldValue, onTextChange, toChecked = false)
                    }
                )
            }
            // Checked checkbox - with smart toggle
            item {
                ToolbarButton(
                    icon = Icons.Default.CheckBox,
                    contentDescription = "Checked checkbox",
                    onClick = {
                        toggleCheckboxOnCurrentLine(textFieldValue, onTextChange, toChecked = true)
                    }
                )
            }
        }
    }
}

/**
 * Insert markdown at cursor position
 */
private fun insertMarkdown(
    current: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    before: String,
    after: String
) {
    val cursorPosition = current.selection.start
    val newText = StringBuilder(current.text)
        .insert(cursorPosition, before + after)
        .toString()

    val newCursorPosition = cursorPosition + before.length
    onTextChange(
        TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    )
}

/**
 * Wrap selected text with markdown syntax
 */
private fun wrapSelection(
    current: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    before: String,
    after: String
) {
    val start = current.selection.start
    val end = current.selection.end

    if (start == end) {
        // No selection - just insert markers
        insertMarkdown(current, onTextChange, before, after)
    } else {
        // Wrap selection
        val selectedText = current.text.substring(start, end)
        val newText = StringBuilder(current.text)
            .replace(start, end, before + selectedText + after)
            .toString()

        val newCursorPosition = end + before.length + after.length
        onTextChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPosition)
            )
        )
    }
}

/**
 * Toggle checkbox on current line between checked/unchecked
 * If cursor is on a checkbox line, toggles it. Otherwise inserts new checkbox.
 */
private fun toggleCheckboxOnCurrentLine(
    current: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    toChecked: Boolean
) {
    val cursorPosition = current.selection.start
    val text = current.text

    // Find the start and end of the current line
    val lineStart = text.lastIndexOf('\n', cursorPosition - 1).let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', cursorPosition).let { if (it == -1) text.length else it }
    val currentLine = text.substring(lineStart, lineEnd)

    // Check if current line is a checkbox (empty or checked)
    val emptyCheckboxRegex = Regex("^- \\[( |)\\] ")
    val checkedCheckboxRegex = Regex("^- \\[[xX]\\] ")

    val newLine = when {
        // Line has empty checkbox - replace with appropriate state
        emptyCheckboxRegex.containsMatchIn(currentLine) -> {
            if (toChecked) {
                currentLine.replace(emptyCheckboxRegex, "- [x] ")
            } else {
                currentLine // Already empty, keep as is
            }
        }
        // Line has checked checkbox - replace with appropriate state
        checkedCheckboxRegex.containsMatchIn(currentLine) -> {
            if (toChecked) {
                currentLine // Already checked, keep as is
            } else {
                currentLine.replace(checkedCheckboxRegex, "- [ ] ")
            }
        }
        // Not a checkbox line - insert new checkbox at cursor
        else -> {
            insertMarkdown(current, onTextChange, if (toChecked) "- [x] " else "- [ ] ", "")
            return
        }
    }

    // Replace the line
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)

    onTextChange(
        TextFieldValue(
            text = newText,
            selection = TextRange(cursorPosition)
        )
    )
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = EInkBlack,
            modifier = Modifier.size(22.dp)
        )
    }
}
