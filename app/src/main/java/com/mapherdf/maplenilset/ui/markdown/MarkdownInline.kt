package com.mapherdf.maplenilset.ui.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.ins.Ins
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
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

private data class RubyData(
    val base: String,
    val annotation: String,
)

internal data class InlineRenderResult(
    val annotatedString: AnnotatedString,
    val inlineContents: Map<String, InlineTextContent>,
)

@Composable
internal fun rememberInlineAnnotated(
    node: Node,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
): InlineRenderResult {
    val rubyContents = remember { mutableStateMapOf<String, RubyData>() }
    val annotated = remember(node) {
        buildAnnotatedString {
            renderInline(node, this, onLinkClick, onTagClick, rubyContents)
        }
    }
    val inlineContents = remember(rubyContents.size) {
        rubyContents.mapValues { (_, ruby) ->
            InlineTextContent(
                placeholder = Placeholder(
                    width = ruby.base.length.sp * 10,
                    height = 48.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                children = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ruby.annotation,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = ruby.base, fontSize = 16.sp)
                    }
                },
            )
        }
    }
    return InlineRenderResult(annotated, inlineContents)
}

private fun renderInline(
    node: Node,
    builder: AnnotatedString.Builder,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    rubyContents: MutableMap<String, RubyData>,
) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is Text -> {
                val literal = child.literal
                if (onTagClick != null && literal.startsWith("#") && literal.length > 1) {
                    val tagEnd = literal.indexOfFirst { it.isWhitespace() }
                    val tag = if (tagEnd == -1) literal else literal.substring(0, tagEnd)
                    builder.withStyle(SpanStyle(color = Color(0xFF7C4DFF), fontWeight = FontWeight.Medium)) { append(tag) }
                    if (tagEnd != -1) builder.append(literal.substring(tagEnd))
                } else {
                    builder.append(literal)
                }
            }
            is StrongEmphasis -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
            }
            is Emphasis -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
            }
            is Strikethrough -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
            }
            is Ins -> builder.withStyle(SpanStyle(background = Color(0xFFFFF59D).copy(alpha = 0.4f))) {
                renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
            }
            is Code -> builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0).copy(alpha = 0.3f))) {
                append(child.literal)
            }
            is Link -> {
                builder.withStyle(SpanStyle(color = Color(0xFF2196F3), textDecoration = TextDecoration.Underline)) {
                    pushStringAnnotation("URL", child.destination)
                    renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
                    pop()
                }
            }
            is Image -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF9E9E9E))) {
                append("[图片: ${child.title ?: "untitled"}]")
            }
            is HardLineBreak, is SoftLineBreak -> builder.append("\n")
            is HtmlInline -> {
                when (child.literal) {
                    "<br>", "<br/>", "<br />" -> builder.append("\n")
                    "<ruby>" -> {
                        val ruby = collectRuby(child)
                        if (ruby != null) {
                            val id = "ruby_${rubyContents.size}"
                            rubyContents[id] = ruby
                            builder.appendInlineContent(id, ruby.base)
                            skipToAfterRuby(child)
                        } else {
                            builder.append(child.literal)
                        }
                    }
                    "<u>" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        renderUntilClosing(child, builder, "</u>", onLinkClick, onTagClick, rubyContents)
                    }
                    "<sub>" -> builder.withStyle(SpanStyle(fontSize = 12.sp, baselineShift = BaselineShift.Subscript)) {
                        renderUntilClosing(child, builder, "</sub>", onLinkClick, onTagClick, rubyContents)
                    }
                    "<sup>" -> builder.withStyle(SpanStyle(fontSize = 12.sp, baselineShift = BaselineShift.Superscript)) {
                        renderUntilClosing(child, builder, "</sup>", onLinkClick, onTagClick, rubyContents)
                    }
                    "<mark>" -> builder.withStyle(SpanStyle(background = Color(0xFFFFF59D).copy(alpha = 0.4f))) {
                        renderUntilClosing(child, builder, "</mark>", onLinkClick, onTagClick, rubyContents)
                    }
                    "<kbd>" -> builder.withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0).copy(alpha = 0.3f))) {
                        renderUntilClosing(child, builder, "</kbd>", onLinkClick, onTagClick, rubyContents)
                    }
                }
            }
            else -> {
                if (child.firstChild != null) {
                    renderInline(child, builder, onLinkClick, onTagClick, rubyContents)
                }
            }
        }
        child = child.next
    }
}

private fun collectRuby(start: HtmlInline): RubyData? {
    var child = start.next
    val base = StringBuilder()
    val annotation = StringBuilder()
    var inAnnotation = false
    while (child != null) {
        when {
            child is HtmlInline && child.literal == "<rt>" -> inAnnotation = true
            child is HtmlInline && child.literal == "</ruby>" -> return RubyData(base.toString(), annotation.toString())
            child is Text -> if (inAnnotation) annotation.append(child.literal) else base.append(child.literal)
        }
        child = child.next
    }
    return null
}

private fun skipToAfterRuby(start: HtmlInline) {
    var node: Node? = start.next
    while (node != null) {
        if (node is HtmlInline && node.literal == "</ruby>") return
        node = node.next
    }
}

private fun renderUntilClosing(
    start: HtmlInline,
    builder: AnnotatedString.Builder,
    closing: String,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    rubyContents: MutableMap<String, RubyData>,
) {
    var node: Node? = start.next
    while (node != null) {
        if (node is HtmlInline && node.literal == closing) return
        when (node) {
            is Text -> builder.append(node.literal)
            is Code -> builder.append(node.literal)
            is StrongEmphasis -> builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            is Emphasis -> builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            is HardLineBreak, is SoftLineBreak -> builder.append("\n")
        }
        if (node is StrongEmphasis || node is Emphasis) builder.pop()
        node = node.next
    }
}
