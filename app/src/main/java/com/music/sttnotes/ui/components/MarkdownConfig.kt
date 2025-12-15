package com.music.sttnotes.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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
 */
@Composable
fun einkMarkdownTypography() = markdownTypography(
    h1 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    h2 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    h3 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    h5 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    paragraph = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.25.sp
    ),
    code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    quote = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.25.sp
    ),
    bullet = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.25.sp
    ),
    ordered = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
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
 * E-Ink optimized Markdown components with proper paragraph spacing
 */
@Composable
fun einkMarkdownComponents(): MarkdownComponents = markdownComponents(
    paragraph = spacedParagraphComponent
)
