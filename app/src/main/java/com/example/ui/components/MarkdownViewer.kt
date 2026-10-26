package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily

sealed interface MarkdownBlock {
    val id: Int

    data class Heading(
        override val id: Int,
        val level: Int,
        val styledText: AnnotatedString
    ) : MarkdownBlock

    data class Paragraph(
        override val id: Int,
        val styledText: AnnotatedString
    ) : MarkdownBlock

    data class Bullet(
        override val id: Int,
        val styledText: AnnotatedString
    ) : MarkdownBlock

    data class Blockquote(
        override val id: Int,
        val styledText: AnnotatedString
    ) : MarkdownBlock

    data class CodeBlock(
        override val id: Int,
        val code: String,
        val language: String
    ) : MarkdownBlock

    data class Image(
        override val id: Int,
        val alt: String,
        val url: String
    ) : MarkdownBlock

    data class HorizontalRule(
        override val id: Int
    ) : MarkdownBlock
}

/**
 * Pre-parses raw Markdown text into structured blocks with precomputed AnnotatedStrings.
 * This ensures parsing and style calculations are only executed once per document,
 * completely off-loading work during scrolling and recompositions.
 */
fun parseMarkdownToBlocks(markdownText: String): List<MarkdownBlock> {
    if (markdownText.isBlank()) return emptyList()

    val lines = markdownText.lines()
    val blocks = ArrayList<MarkdownBlock>(minOf(lines.size, 500))
    var blockId = 0

    var inCodeBlock = false
    val codeBuilder = StringBuilder()
    var codeLang = ""

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // Check for code fences
        if (line.trimStart().startsWith("```")) {
            if (inCodeBlock) {
                // Close code block
                val content = codeBuilder.toString().trimEnd()
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        id = blockId++,
                        code = content,
                        language = codeLang
                    )
                )
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = line.trimStart().removePrefix("```").trim()
            }
            i++
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append('\n')
            i++
            continue
        }

        val trimmed = line.trim()

        when {
            trimmed.isEmpty() -> {
                // Ignore blank lines to reduce unnecessary Composable node counts
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MarkdownBlock.HorizontalRule(id = blockId++))
            }
            trimmed.startsWith("# ") -> {
                blocks.add(
                    MarkdownBlock.Heading(
                        id = blockId++,
                        level = 1,
                        styledText = parseInlineMarkdown(trimmed.removePrefix("# ").trim())
                    )
                )
            }
            trimmed.startsWith("## ") -> {
                blocks.add(
                    MarkdownBlock.Heading(
                        id = blockId++,
                        level = 2,
                        styledText = parseInlineMarkdown(trimmed.removePrefix("## ").trim())
                    )
                )
            }
            trimmed.startsWith("### ") -> {
                blocks.add(
                    MarkdownBlock.Heading(
                        id = blockId++,
                        level = 3,
                        styledText = parseInlineMarkdown(trimmed.removePrefix("### ").trim())
                    )
                )
            }
            trimmed.startsWith("#### ") -> {
                blocks.add(
                    MarkdownBlock.Heading(
                        id = blockId++,
                        level = 4,
                        styledText = parseInlineMarkdown(trimmed.removePrefix("#### ").trim())
                    )
                )
            }
            trimmed.startsWith("> ") -> {
                blocks.add(
                    MarkdownBlock.Blockquote(
                        id = blockId++,
                        styledText = parseInlineMarkdown(trimmed.removePrefix("> ").trim())
                    )
                )
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks.add(
                    MarkdownBlock.Bullet(
                        id = blockId++,
                        styledText = parseInlineMarkdown(trimmed.substring(2).trim())
                    )
                )
            }
            trimmed.startsWith("![") && trimmed.contains("](") && trimmed.endsWith(")") -> {
                val url = trimmed.substringAfter("](").removeSuffix(")")
                val alt = trimmed.substringAfter("![").substringBefore("]")
                blocks.add(
                    MarkdownBlock.Image(
                        id = blockId++,
                        alt = alt,
                        url = url
                    )
                )
            }
            else -> {
                blocks.add(
                    MarkdownBlock.Paragraph(
                        id = blockId++,
                        styledText = parseInlineMarkdown(trimmed)
                    )
                )
            }
        }
        i++
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        blocks.add(
            MarkdownBlock.CodeBlock(
                id = blockId++,
                code = codeBuilder.toString().trimEnd(),
                language = codeLang
            )
        )
    }

    return blocks
}

@Composable
fun MarkdownViewer(
    markdownText: String,
    modifier: Modifier = Modifier,
    initialBlockLimit: Int = 100
) {
    if (markdownText.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No README documentation provided for this repository.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Memoize the parsing of markdown text into blocks
    val blocks = remember(markdownText) {
        parseMarkdownToBlocks(markdownText)
    }

    var isExpanded by remember(markdownText) { mutableStateOf(false) }

    val displayedBlocks = if (isExpanded || blocks.size <= initialBlockLimit) {
        blocks
    } else {
        blocks.take(initialBlockLimit)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        displayedBlocks.forEach { block ->
            MarkdownBlockItem(block = block)
        }

        if (!isExpanded && blocks.size > initialBlockLimit) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = { isExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Show Full README (${blocks.size - initialBlockLimit} more sections)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun MarkdownBlockItem(
    block: MarkdownBlock,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    when (block) {
        is MarkdownBlock.Heading -> {
            when (block.level) {
                1 -> {
                    Text(
                        text = block.styledText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        modifier = modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                2 -> {
                    Text(
                        text = block.styledText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        modifier = modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                3 -> {
                    Text(
                        text = block.styledText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        modifier = modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                else -> {
                    Text(
                        text = block.styledText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = PlusJakartaSansFamily
                        ),
                        modifier = modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
            }
        }
        is MarkdownBlock.Paragraph -> {
            Text(
                text = block.styledText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = PlusJakartaSansFamily
                ),
                modifier = modifier.padding(vertical = 3.dp)
            )
        }
        is MarkdownBlock.Bullet -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = block.styledText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = PlusJakartaSansFamily
                    )
                )
            }
        }
        is MarkdownBlock.Blockquote -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = block.styledText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFamily
                    )
                )
            }
        }
        is MarkdownBlock.CodeBlock -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = block.language.ifBlank { "code" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(block.code)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
        is MarkdownBlock.Image -> {
            AsyncImage(
                model = block.url,
                contentDescription = block.alt.ifBlank { "Embedded image" },
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Inside
            )
        }
        is MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * Basic inline parser for **bold**, *italic*, `code`, and [links](url)
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            when {
                // Bold: **text**
                text.startsWith("**", cursor) -> {
                    val end = text.indexOf("**", cursor + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(cursor + 2, end))
                        }
                        cursor = end + 2
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Inline code: `code`
                text.startsWith("`", cursor) -> {
                    val end = text.indexOf("`", cursor + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFamily,
                                background = Color(0x22888888),
                                fontSize = 12.sp
                            )
                        ) {
                            append(" " + text.substring(cursor + 1, end) + " ")
                        }
                        cursor = end + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Link: [label](url)
                text.startsWith("[", cursor) -> {
                    val labelEnd = text.indexOf("](", cursor)
                    val urlEnd = if (labelEnd != -1) text.indexOf(")", labelEnd) else -1
                    if (labelEnd != -1 && urlEnd != -1) {
                        val label = text.substring(cursor + 1, labelEnd)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF6366F1),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(label)
                        }
                        cursor = urlEnd + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                // Italic: *text*
                text.startsWith("*", cursor) -> {
                    val end = text.indexOf("*", cursor + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(cursor + 1, end))
                        }
                        cursor = end + 1
                    } else {
                        append(text[cursor])
                        cursor++
                    }
                }
                else -> {
                    append(text[cursor])
                    cursor++
                }
            }
        }
    }
}
