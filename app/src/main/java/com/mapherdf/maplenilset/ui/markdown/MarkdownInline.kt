package com.mapherdf.maplenilset.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.ext.ins.Ins
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.node.AbstractVisitor

internal class MarkdownTextCollector : AbstractVisitor() {
    val builder = StringBuilder()
}

internal fun Node.collectText(): String {
    val builder = StringBuilder()
    fun walk(node: Node) {
        var child = node.firstChild
        while (child != null) {
            when (child) {
                is Text -> builder.append(child.literal)
                is Code -> builder.append(child.literal)
                is SoftLineBreak, is HardLineBreak -> builder.append("\n")
                else -> walk(child)
            }
            child = child.next
        }
    }
    walk(this)
    return builder.toString()
}

@Composable
internal fun rememberInlineAnnotated(
    node: Node,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
): AnnotatedString {
    return remember(node) {
        buildAnnotatedString {
            renderInline(node, this, onLinkClick, onTagClick)
        }
    }
}

private fun renderInline(
    node: Node,
    builder: AnnotatedString.Builder,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is Text -> {
                val literal = child.literal
                if (onTagClick != null && literal.startsWith("#") && literal.length > 1) {
                    val tagEnd = literal.indexOfFirst { it.isWhitespace() }
                    val tag = if (tagEnd == -1) literal else literal.substring(0, tagEnd)
                    builder.withStyle(
                        SpanStyle(
                            color = Color(0xFF7C4DFF),
                            fontWeight = FontWeight.Medium,
                        ),
                    ) {
                        append(tag)
                    }
                    if (tagEnd != -1) builder.append(literal.substring(tagEnd))
                } else {
                    builder.append(literal)
                }
            }
            is StrongEmphasis -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    renderInline(child, builder, onLinkClick, onTagClick)
                }
            }
            is Emphasis -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    renderInline(child, builder, onLinkClick, onTagClick)
                }
            }
            is Strikethrough -> {
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    renderInline(child, builder, onLinkClick, onTagClick)
                }
            }
            is Ins -> {
                builder.withStyle(
                    SpanStyle(background = Color(0xFFFFF59D).copy(alpha = 0.4f)),
                ) {
                    renderInline(child, builder, onLinkClick, onTagClick)
                }
            }
            is Code -> {
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFFE0E0E0).copy(alpha = 0.3f),
                    ),
                ) {
                    append(child.literal)
                }
            }
            is Link -> {
                val url = child.destination
                builder.withStyle(
                    SpanStyle(
                        color = Color(0xFF2196F3),
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    pushStringAnnotation("URL", url)
                    renderInline(child, builder, onLinkClick, onTagClick)
                    pop()
                }
            }
            is Image -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF9E9E9E))) {
                    append("[图片: ${child.title ?: "untitled"}]")
                }
            }
            is HardLineBreak, is SoftLineBreak -> builder.append("\n")
            else -> {
                if (child.firstChild != null) {
                    renderInline(child, builder, onLinkClick, onTagClick)
                }
            }
        }
        child = child.next
    }
}

