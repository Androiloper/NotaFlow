package com.example.notaflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notaflow.ui.components.TextStyle as FormatStyle

/**
 * True WYSIWYG rich text editor that shows formatting as you type.
 * The editor's content is Markdown.
 */
@Composable
fun RichTextEditor(
    initialContent: String,
    onMarkdownChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    readOnly: Boolean = false,
    placeholderText: String = "Start typing here..."
) {

    val hapticFeedback = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val editorScrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    )

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent, selection = TextRange(initialContent.length)))
    }

    var activeFormats by remember { mutableStateOf(setOf<FormatStyle>()) }

    // State for link dialog
    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }

    // Define colors for the transformation based on the current theme
    val transformationColors = RichTextTransformation.Colors(
        h1 = MaterialTheme.colorScheme.primary,
        h2 = MaterialTheme.colorScheme.secondary,
        h3 = MaterialTheme.colorScheme.tertiary,
        quoteBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        quoteBorder = MaterialTheme.colorScheme.outlineVariant,
        list = MaterialTheme.colorScheme.tertiary,
        link = MaterialTheme.colorScheme.primary,
        codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        codeText = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val richTextTransformation = remember(transformationColors) {
        RichTextTransformation(transformationColors)
    }

    val applyFormatting = remember {
        applyFormatting@{ style: FormatStyle ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val selection = textFieldValue.selection

            if (!selection.collapsed) {
                // Fix: Add safe bounds checking for selection indices
                val safeStart = selection.start.coerceIn(0, textFieldValue.text.length)
                val safeEnd = selection.end.coerceIn(0, textFieldValue.text.length)


                if (safeStart < safeEnd)
                    selectedText = textFieldValue.text.substring(safeStart, safeEnd)
                    if (style == FormatStyle.LINK){
                        showLinkDialog = true
                        return@applyFormatting
                    }

                if (style == FormatStyle.LINK) {
                    showLinkDialog = true
                    return@applyFormatting
                }

                val prefix = textFieldValue.text.substring(0, safeStart)
                val suffix = textFieldValue.text.substring(safeEnd)

                if (style == FormatStyle.NORMAL) {
                    activeFormats = emptySet()
                } else {
                    val markdownFormatted = applyMarkdownFormat(selectedText, style, textFieldValue.text, selection)
                    val newText = prefix + markdownFormatted + suffix
                    val newSelectionStart = prefix.length + markdownFormatted.length
                    textFieldValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newSelectionStart)
                    )
                    onMarkdownChanged(newText)
                }
            } else {
                activeFormats = if (style == FormatStyle.NORMAL) {
                    emptySet()
                } else if (activeFormats.contains(style)) {
                    activeFormats - style
                } else {
                    val newActive = activeFormats.toMutableSet()
                    when (style) {
                        FormatStyle.HEADING1, FormatStyle.HEADING2, FormatStyle.HEADING3 -> {
                            newActive.removeAll(setOf(FormatStyle.HEADING1, FormatStyle.HEADING2, FormatStyle.HEADING3))
                            newActive.add(style)
                        }
                        else -> newActive.add(style)
                    }
                    newActive
                }
            }
            focusRequester.requestFocus()
        }
    }

    // Handle link dialog confirmation
    if (showLinkDialog) {
        LinkDialog(
            initialText = selectedText,
            isVisible = true,
            onDismiss = { showLinkDialog = false },
            onConfirm = { text, url ->
                val selection = textFieldValue.selection
                // Use safe indices
                val safeStart = selection.start.coerceIn(0, textFieldValue.text.length)
                val safeEnd = selection.end.coerceIn(0, textFieldValue.text.length)

                // Only proceed if selection is valid
                if (safeStart <= safeEnd) {
                    val prefix = textFieldValue.text.substring(0, safeStart)
                    val suffix = if (safeEnd < textFieldValue.text.length) {
                        textFieldValue.text.substring(safeEnd)
                    } else {
                        ""
                    }

                    // Create markdown link
                    val markdownLink = "[$text]($url)"
                    val newText = prefix + markdownLink + suffix
                    val newSelectionStart = prefix.length + markdownLink.length

                    textFieldValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newSelectionStart)
                    )
                    onMarkdownChanged(newText)
                }
                showLinkDialog = false
            }
        )
    }

    LaunchedEffect(readOnly) {
        if (!readOnly) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) { /* Ignore */ }
        } else {
            focusManager.clearFocus()
        }
    }

    Column(modifier = modifier) {
        if (showToolbar && !readOnly) {
            RichTextFormatToolbar(
                onStyleSelected = applyFormatting,
                activeStyles = activeFormats,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(editorScrollState)
            ) {
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onMarkdownChanged(newValue.text)
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Default,
                            lineHeight = 24.sp
                        ),
                        visualTransformation = richTextTransformation,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        readOnly = readOnly
                    )

                    if (textFieldValue.text.isEmpty() && !readOnly) {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual transformation that converts markdown to rich text with correct offset mapping.
 * Uses injected theme colors for styling.
 */
private class RichTextTransformation(private val colors: Colors) : VisualTransformation {

    @Stable // Mark as stable if all constructor params are stable
    data class Colors(
        val h1: Color,
        val h2: Color,
        val h3: Color,
        val quoteBackground: Color,
        val quoteBorder: Color,
        val list: Color,
        val link: Color,
        val codeBackground: Color,
        val codeText: Color
    )

    data class HiddenRange(val start: Int, val length: Int) : Comparable<HiddenRange> {
        override fun compareTo(other: HiddenRange): Int = start.compareTo(other.start)
    }

    private var lastOriginalText: String? = null
    private var lastTransformedText: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == lastOriginalText && lastTransformedText != null) {
            return lastTransformedText!!
        }

        val originalText = text.text
        val hiddenRanges = mutableListOf<HiddenRange>()
        val annotatedString = buildAnnotatedStringWithGaps(originalText, hiddenRangesCollector = hiddenRanges)

        val offsetMapping = MarkdownOffsetMapping(
            originalLength = originalText.length,
            transformedStringLength = annotatedString.length,
            sortedHiddenRanges = hiddenRanges.sorted()
        )

        val result = TransformedText(annotatedString, offsetMapping)

        lastOriginalText = originalText
        lastTransformedText = result
        return result
    }

    private fun buildAnnotatedStringWithGaps(
        markdown: String,
        hiddenRangesCollector: MutableList<HiddenRange>
    ): AnnotatedString {
        return buildAnnotatedString {
            val lines = markdown.lines()
            var currentOriginalIndex = 0

            for ((lineIndex, line) in lines.withIndex()) {
                val lineStartOriginalIndex = currentOriginalIndex

                fun addHiddenRange(relativeStart: Int, length: Int) {
                    if (length > 0) {
                        hiddenRangesCollector.add(HiddenRange(lineStartOriginalIndex + relativeStart, length))
                    }
                }

                var consumedInLine = 0
                val blockParseResult = parseBlockElement(line, lineStartOriginalIndex, hiddenRangesCollector)

                if (blockParseResult != null) {
                    withStyle(blockParseResult.style) { append(blockParseResult.visualText) }
                    append(blockParseResult.contentText)
                    consumedInLine = line.length
                } else {
                    var remainingLinePart = line
                    var currentRelativeInlineIndex = 0

                    while (remainingLinePart.isNotEmpty()) {
                        val inlineMatch = findFirstInlineMarkdown(remainingLinePart)
                        if (inlineMatch != null) {
                            if (inlineMatch.matchStartIndexInChunk > 0) {
                                append(remainingLinePart.substring(0, inlineMatch.matchStartIndexInChunk))
                            }
                            addHiddenRange(
                                consumedInLine + currentRelativeInlineIndex + inlineMatch.matchStartIndexInChunk,
                                inlineMatch.openingMarker.length
                            )
                            withStyle(inlineMatch.style) { append(inlineMatch.contentText) }
                            addHiddenRange(
                                consumedInLine + currentRelativeInlineIndex + inlineMatch.matchStartIndexInChunk +
                                        inlineMatch.openingMarker.length + inlineMatch.contentText.length,
                                inlineMatch.closingMarker.length
                            )
                            val consumedByMatch = inlineMatch.matchStartIndexInChunk +
                                    inlineMatch.openingMarker.length +
                                    inlineMatch.contentText.length +
                                    inlineMatch.closingMarker.length
                            currentRelativeInlineIndex += consumedByMatch
                            remainingLinePart = remainingLinePart.substring(consumedByMatch)
                        } else {
                            append(remainingLinePart)
                            currentRelativeInlineIndex += remainingLinePart.length
                            remainingLinePart = ""
                        }
                    }
                    consumedInLine += currentRelativeInlineIndex
                }
                currentOriginalIndex += line.length

                if (lineIndex < lines.size - 1) {
                    append("\n")
                    currentOriginalIndex += 1
                }
            }
        }
    }

    private data class BlockParseResult(
        val style: SpanStyle,
        val visualText: String,
        val contentText: String
    )

    private fun parseBlockElement(
        line: String,
        lineStartOriginalIndex: Int,
        hiddenRangesCollector: MutableList<HiddenRange>
    ): BlockParseResult? {
        fun addHidden(length: Int) {
            if (length > 0) hiddenRangesCollector.add(HiddenRange(lineStartOriginalIndex, length))
        }

        return when {
            // Handle headers (using regex for exact matching)
            line.matches(Regex("^#\\s+.*$")) -> {
                addHidden(2);
                BlockParseResult(
                    SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.h1),
                    "",
                    line.substring(2)
                )
            }
            line.matches(Regex("^##\\s+.*$")) -> {
                addHidden(3);
                BlockParseResult(
                    SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.h2),
                    "",
                    line.substring(3)
                )
            }
            line.matches(Regex("^###\\s+.*$")) -> {
                addHidden(4);
                BlockParseResult(
                    SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.h3),
                    "",
                    line.substring(4)
                )
            }

            // Blockquotes
            line.startsWith("> ") -> {
                addHidden(2)
                BlockParseResult(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        background = colors.quoteBackground,
                    ),
                    "❝ ", // Visual prefix in the styled block
                    line.substring(2) + " ❞" // Content
                )
            }

            // Bullet lists
            line.matches(Regex("^[*+-]\\s+.*$")) -> {
                addHidden(2);
                BlockParseResult(
                    SpanStyle(color = colors.list),
                    "•  ",
                    line.substring(2)
                )
            }

            // Numbered lists
            line.matches(Regex("^\\d+\\.\\s+.*$")) -> {
                val markerMatch = Regex("^(\\d+\\.)\\s+").find(line)
                markerMatch?.let {
                    val marker = it.value
                    addHidden(marker.length)
                    BlockParseResult(
                        SpanStyle(color = colors.list, fontWeight = FontWeight.SemiBold),
                        marker,
                        line.substring(marker.length)
                    )
                }
            }

            else -> null
        }
    }

    private data class InlineMatch(
        val matchStartIndexInChunk: Int,
        val openingMarker: String,
        val contentText: String,
        val closingMarker: String,
        val style: SpanStyle,
        val fullMatchLength: Int
    )

    private data class PatternRule(
        val regex: Regex,
        val openingMarkerSyntaxBuilder: (MatchResult) -> String = { "" },
        val contentTextBuilder: (MatchResult) -> String,
        val closingMarkerSyntaxBuilder: (MatchResult) -> String = { "" },
        val styleProvider: (MatchResult) -> SpanStyle
    )

    private fun findFirstInlineMarkdown(lineChunk: String): InlineMatch? {
        val patterns = listOf(
            PatternRule(
                regex = Regex("\\[([^]]+)]\\(([^)]*)\\)"),
                openingMarkerSyntaxBuilder = { "[" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "](${it.groupValues[2]})" },
                styleProvider = { SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline) }
            ),
            PatternRule(
                regex = Regex("(?<!\\\\)\\*\\*(?!\\s)(.*?)(?<!\\s)(?<!\\\\)\\*\\*"),
                openingMarkerSyntaxBuilder = { "**" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "**" },
                styleProvider = { SpanStyle(fontWeight = FontWeight.Bold) }
            ),
            PatternRule(
                regex = Regex("(?<!\\\\)_(?!\\s)(.*?)(?<!\\s)(?<!\\\\)_"),
                openingMarkerSyntaxBuilder = { "_" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "_" },
                styleProvider = { SpanStyle(fontStyle = FontStyle.Italic) }
            ),
            PatternRule(
                regex = Regex("(?<![\\\\*])\\*(?!\\s|\\*)(.*?)(?<!\\s)(?<![\\\\*])\\*(?![*])"),
                openingMarkerSyntaxBuilder = { "*" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "*" },
                styleProvider = { SpanStyle(fontStyle = FontStyle.Italic) }
            ),
            PatternRule(
                regex = Regex("(?<!\\\\)~~(?!\\s)(.*?)(?<!\\s)(?<!\\\\)~~"),
                openingMarkerSyntaxBuilder = { "~~" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "~~" },
                styleProvider = { SpanStyle(textDecoration = TextDecoration.LineThrough) }
            ),
            PatternRule(
                regex = Regex("<u>(?!\\s)(.*?)(?<!\\s)</u>"),
                openingMarkerSyntaxBuilder = { "<u>" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "</u>" },
                styleProvider = { SpanStyle(textDecoration = TextDecoration.Underline) }
            ),
            PatternRule(
                regex = Regex("(?<!\\\\)`(?!\\s)(.*?)(?<!\\s)(?<!\\\\)`"),
                openingMarkerSyntaxBuilder = { "`" },
                contentTextBuilder = { it.groupValues[1] },
                closingMarkerSyntaxBuilder = { "`" },
                styleProvider = { SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground, color = colors.codeText, fontSize = 14.sp) }
            )
        )

        var earliestMatch: InlineMatch? = null
        for (rule in patterns) {
            rule.regex.findAll(lineChunk).forEach { matchResult ->
                val candidate = InlineMatch(
                    matchStartIndexInChunk = matchResult.range.first,
                    openingMarker = rule.openingMarkerSyntaxBuilder(matchResult),
                    contentText = rule.contentTextBuilder(matchResult),
                    closingMarker = rule.closingMarkerSyntaxBuilder(matchResult),
                    style = rule.styleProvider(matchResult),
                    fullMatchLength = matchResult.value.length
                )

                if (earliestMatch == null ||
                    candidate.matchStartIndexInChunk < earliestMatch!!.matchStartIndexInChunk ||
                    (candidate.matchStartIndexInChunk == earliestMatch!!.matchStartIndexInChunk &&
                            candidate.fullMatchLength > earliestMatch!!.fullMatchLength)) {
                    earliestMatch = candidate
                }
            }
        }

        return earliestMatch
    }

    private class MarkdownOffsetMapping(
        private val originalLength: Int,
        private val transformedStringLength: Int,
        private val sortedHiddenRanges: List<HiddenRange>
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var charsToSubtract = 0
            for (range in sortedHiddenRanges) {
                if (offset > range.start) {
                    if (offset >= range.start + range.length) {
                        charsToSubtract += range.length
                    } else {
                        charsToSubtract += (offset - range.start)
                        break
                    }
                } else {
                    break
                }
            }
            return (offset - charsToSubtract).coerceIn(0, transformedStringLength)
        }

        override fun transformedToOriginal(offset: Int): Int {
            var originalOffset = offset
            var accumulatedHiddenLengthBeforeTransformedPos = 0
            for (range in sortedHiddenRanges) {
                val hiddenRangeStartInTransformed = range.start - accumulatedHiddenLengthBeforeTransformedPos
                if (hiddenRangeStartInTransformed < offset) {
                    originalOffset += range.length
                    accumulatedHiddenLengthBeforeTransformedPos += range.length
                } else {
                    break
                }
            }
            return originalOffset.coerceIn(0, originalLength)
        }
    }
}

/**
 * Applies the selected markdown format to the given text.
 */
private fun applyMarkdownFormat(text: String, style: FormatStyle, fullText: String, selection: TextRange): String {
    return when (style) {
        FormatStyle.BOLD -> "**$text**"
        FormatStyle.ITALIC -> "_${text}_"
        FormatStyle.UNDERLINE -> "<u>$text</u>"
        FormatStyle.STRIKETHROUGH -> "~~$text~~"
        FormatStyle.HEADING1 -> {
            // Check if we need to prepend a newline
            val needsNewline = selection.start > 0 &&
                    fullText[selection.start - 1] != '\n' &&
                    !text.startsWith("\n")
            val prefix = if (needsNewline) "\n" else ""

            // Apply heading to each line
            val lines = text.lines()
            prefix + lines.joinToString("\n") {
                if (it.startsWith("# ")) it else "# $it"
            }
        }
        FormatStyle.HEADING2 -> {
            val needsNewline = selection.start > 0 &&
                    fullText[selection.start - 1] != '\n' &&
                    !text.startsWith("\n")
            val prefix = if (needsNewline) "\n" else ""

            prefix + text.lines().joinToString("\n") {
                if (it.startsWith("## ")) it else "## $it"
            }
        }
        FormatStyle.HEADING3 -> {
            val needsNewline = selection.start > 0 &&
                    fullText[selection.start - 1] != '\n' &&
                    !text.startsWith("\n")
            val prefix = if (needsNewline) "\n" else ""

            prefix + text.lines().joinToString("\n") {
                if (it.startsWith("### ")) it else "### $it"
            }
        }
        FormatStyle.BULLET_LIST -> text.lines().joinToString("\n") {
            if (it.matches(Regex("^[*+-]\\s+.*$"))) it else "* $it"
        }
        FormatStyle.NUMBERED_LIST -> text.lines().mapIndexed { i, line ->
            if (line.matches(Regex("^\\d+\\.\\s+.*$"))) line else "${i + 1}. $line"
        }.joinToString("\n")
        FormatStyle.QUOTE -> text.lines().joinToString("\n") {
            if (it.startsWith("> ")) it else "> $it"
        }
        FormatStyle.CODE -> "`$text`"
        FormatStyle.LINK -> "[$text](https://)"
        FormatStyle.NORMAL -> text
    }
}

@Composable
fun RichTextFormatToolbar(
    onStyleSelected: (FormatStyle) -> Unit,
    activeStyles: Set<FormatStyle>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatButtonWithState(
                    text = "H1",
                    tooltip = "Heading 1",
                    isActive = activeStyles.contains(FormatStyle.HEADING1),
                    onClick = { onStyleSelected(FormatStyle.HEADING1) }
                )
                FormatButtonWithState(
                    text = "H2",
                    tooltip = "Heading 2",
                    isActive = activeStyles.contains(FormatStyle.HEADING2),
                    onClick = { onStyleSelected(FormatStyle.HEADING2) }
                )
                FormatButtonWithState(
                    text = "H3",
                    tooltip = "Heading 3",
                    isActive = activeStyles.contains(FormatStyle.HEADING3),
                    onClick = { onStyleSelected(FormatStyle.HEADING3) }
                )
                VerticalDivider()
                FormatButtonWithState(
                    text = "B",
                    tooltip = "Bold",
                    isBold = true,
                    isActive = activeStyles.contains(FormatStyle.BOLD),
                    onClick = { onStyleSelected(FormatStyle.BOLD) }
                )
                FormatButtonWithState(
                    text = "I",
                    tooltip = "Italic",
                    isItalic = true,
                    isActive = activeStyles.contains(FormatStyle.ITALIC),
                    onClick = { onStyleSelected(FormatStyle.ITALIC) }
                )
                FormatButtonWithState(
                    text = "U",
                    tooltip = "Underline",
                    isUnderlined = true,
                    isActive = activeStyles.contains(FormatStyle.UNDERLINE),
                    onClick = { onStyleSelected(FormatStyle.UNDERLINE) }
                )
                FormatButtonWithState(
                    text = "S",
                    tooltip = "Strikethrough",
                    isStrikethrough = true,
                    isActive = activeStyles.contains(FormatStyle.STRIKETHROUGH),
                    onClick = { onStyleSelected(FormatStyle.STRIKETHROUGH) }
                )
                VerticalDivider()
                FormatButtonWithState(
                    text = "•",
                    tooltip = "Bullet List",
                    isActive = activeStyles.contains(FormatStyle.BULLET_LIST),
                    onClick = { onStyleSelected(FormatStyle.BULLET_LIST) }
                )
                FormatButtonWithState(
                    text = "1.",
                    tooltip = "Numbered List",
                    isActive = activeStyles.contains(FormatStyle.NUMBERED_LIST),
                    onClick = { onStyleSelected(FormatStyle.NUMBERED_LIST) }
                )
                VerticalDivider()
                FormatButtonWithState(
                    text = "\"",
                    tooltip = "Quote",
                    isActive = activeStyles.contains(FormatStyle.QUOTE),
                    onClick = { onStyleSelected(FormatStyle.QUOTE) }
                )
                FormatButtonWithState(
                    text = "</>",
                    tooltip = "Code",
                    isActive = activeStyles.contains(FormatStyle.CODE),
                    onClick = { onStyleSelected(FormatStyle.CODE) }
                )
                FormatButtonWithState(
                    text = "🔗",
                    tooltip = "Link",
                    isActive = activeStyles.contains(FormatStyle.LINK),
                    onClick = { onStyleSelected(FormatStyle.LINK) }
                )
                VerticalDivider()
                FormatButtonWithState(
                    text = "Tx",
                    tooltip = "Normal Text",
                    isActive = activeStyles.isEmpty() || activeStyles.contains(FormatStyle.NORMAL),
                    onClick = { onStyleSelected(FormatStyle.NORMAL) }
                )
            }
            Text(
                text = "Select text to format, or toggle styles for new text.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun FormatButtonWithState(
    text: String,
    tooltip: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderlined: Boolean = false,
    isStrikethrough: Boolean = false,
    modifier: Modifier = Modifier
) {
    val buttonBackgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(buttonBackgroundColor)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                isUnderlined && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                isUnderlined -> TextDecoration.Underline
                isStrikethrough -> TextDecoration.LineThrough
                else -> TextDecoration.None
            },
            fontSize = 14.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Divider(
        modifier = Modifier
            .padding(vertical = 6.dp, horizontal = 6.dp)
            .height(20.dp)
            .width(1.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}