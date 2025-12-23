package com.music.sttnotes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.music.sttnotes.ui.theme.EInkBlack
import com.music.sttnotes.ui.theme.EInkGrayLight
import com.music.sttnotes.ui.theme.EInkGrayLighter
import com.music.sttnotes.ui.theme.EInkGrayMedium

/**
 * E-Ink optimized Markdown typography for Knowledge Base detail view
 * Larger fonts for comfortable reading on e-ink displays
 * @param fontSizeMultiplier Multiplier for all font sizes (default 1.0, range 0.7-1.3 recommended)
 */
@Composable
fun einkMarkdownTypography(fontSizeMultiplier: Float = 1.0f) = markdownTypography(
    h1 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (28 * fontSizeMultiplier).sp,
        lineHeight = (36 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    h2 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (24 * fontSizeMultiplier).sp,
        lineHeight = (32 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    h3 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20 * fontSizeMultiplier).sp,
        lineHeight = (28 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18 * fontSizeMultiplier).sp,
        lineHeight = (26 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    h5 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = (16 * fontSizeMultiplier).sp,
        lineHeight = (24 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = (15 * fontSizeMultiplier).sp,
        lineHeight = (22 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    paragraph = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (17 * fontSizeMultiplier).sp,
        lineHeight = (28 * fontSizeMultiplier).sp,
        letterSpacing = 0.25.sp
    ),
    code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * fontSizeMultiplier).sp,
        lineHeight = (20 * fontSizeMultiplier).sp,
        letterSpacing = 0.sp
    ),
    quote = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * fontSizeMultiplier).sp,
        lineHeight = (26 * fontSizeMultiplier).sp,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.25.sp
    ),
    bullet = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (17 * fontSizeMultiplier).sp,
        lineHeight = (28 * fontSizeMultiplier).sp,
        letterSpacing = 0.25.sp
    ),
    ordered = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (17 * fontSizeMultiplier).sp,
        lineHeight = (28 * fontSizeMultiplier).sp,
        letterSpacing = 0.25.sp
    )
)

/**
 * Chat-specific Markdown typography - configurable font size
 * @param baseFontSize The base font size in sp (default 14)
 */
@Composable
fun chatMarkdownTypography(baseFontSize: Float = 14f) = markdownTypography(
    h1 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (baseFontSize + 6).sp,
        lineHeight = (baseFontSize + 12).sp
    ),
    h2 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (baseFontSize + 4).sp,
        lineHeight = (baseFontSize + 10).sp
    ),
    h3 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (baseFontSize + 2).sp,
        lineHeight = (baseFontSize + 8).sp
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (baseFontSize + 1).sp,
        lineHeight = (baseFontSize + 6).sp
    ),
    h5 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = baseFontSize.sp,
        lineHeight = (baseFontSize + 4).sp
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = (baseFontSize - 1).sp,
        lineHeight = (baseFontSize + 4).sp
    ),
    paragraph = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = baseFontSize.sp,
        lineHeight = (baseFontSize + 8).sp,
        letterSpacing = 0.25.sp
    ),
    code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = (baseFontSize - 2).sp,
        lineHeight = (baseFontSize + 4).sp
    ),
    quote = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = baseFontSize.sp,
        lineHeight = (baseFontSize + 6).sp,
        fontStyle = FontStyle.Italic
    ),
    bullet = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = baseFontSize.sp,
        lineHeight = (baseFontSize + 8).sp
    ),
    ordered = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = baseFontSize.sp,
        lineHeight = (baseFontSize + 8).sp
    )
)

/**
 * E-Ink optimized Markdown colors - high contrast black on white
 */
@Composable
fun einkMarkdownColors() = markdownColor(
    text = EInkBlack,
    codeText = EInkBlack,
    codeBackground = EInkGrayLighter,
    inlineCodeText = EInkBlack,
    inlineCodeBackground = EInkGrayLight,
    linkText = EInkBlack,
    dividerColor = EInkGrayMedium
)

/**
 * Custom paragraph component with proper spacing between paragraphs
 * Adds bottom padding to match edit mode spacing
 */
private val spacedParagraphComponent: MarkdownComponent = {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        MarkdownParagraph(it.content, it.node)
    }
}

/**
 * Custom horizontal rule component with proper spacing
 * Adds top and bottom padding to ensure visibility
 */
private val spacedHorizontalRuleComponent: MarkdownComponent = {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = EInkGrayMedium
        )
    }
}

/**
 * E-Ink optimized Markdown components with proper paragraph spacing
 */
@Composable
fun einkMarkdownComponents(): MarkdownComponents = markdownComponents(
    paragraph = spacedParagraphComponent
)

/**
 * Convert checkbox syntax to Unicode checkboxes for rendering
 * Converts:
 * - [ ] or - [] → ◯ (large empty circle)
 * - [x] or [X] → ⬤ (large filled circle)
 * Removes the dash/bullet to show only the checkbox symbol
 */
fun convertCheckboxesToUnicode(markdown: String): String {
    return markdown
        .replace(Regex("^- \\[x\\] ", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "⬤ ")
        .replace(Regex("^- \\[X\\] ", RegexOption.MULTILINE), "⬤ ")
        .replace(Regex("^- \\[ \\] ", RegexOption.MULTILINE), "◯ ")
        .replace(Regex("^- \\[\\] ", RegexOption.MULTILINE), "◯ ")
}
