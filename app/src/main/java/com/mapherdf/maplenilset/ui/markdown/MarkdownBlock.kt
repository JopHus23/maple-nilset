package com.mapherdf.maplenilset.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Node
import org.commonmark.ext.task.list.items.TaskListItemMarker

@Composable
internal fun MarkdownBlock(
    node: Node,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    onTaskToggle: ((Int, Boolean) -> Unit)?,
) {
    when (node) {
        is Heading -> HeadingBlock(node)
        is Paragraph -> InlineText(node, onLinkClick, onTagClick)
        is FencedCodeBlock -> CodeBlock(node)
        is IndentedCodeBlock -> CodeBlock(node.literal)
        is BlockQuote -> BlockQuoteBlock(node, onLinkClick, onTagClick, onTaskToggle)
        is BulletList -> BulletListBlock(node, onLinkClick, onTagClick, onTaskToggle)
        is OrderedList -> OrderedListBlock(node, onLinkClick, onTagClick, onTaskToggle)
        is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        else -> {
            if (node.firstChild != null) {
                InlineText(node, onLinkClick, onTagClick)
            }
        }
    }
}

@Composable
private fun HeadingBlock(heading: Heading) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        5 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val text = heading.collectText()
    Text(
        text = text,
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun InlineText(
    node: Node,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
) {
    val result = rememberInlineAnnotated(node, onLinkClick, onTagClick)
    Text(
        text = result.annotatedString,
        inlineContent = result.inlineContents,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun CodeBlock(code: FencedCodeBlock) {
    CodeBlockContent(code.literal, code.info)
}

@Composable
private fun CodeBlock(code: String) {
    CodeBlockContent(code, null)
}

@Composable
private fun CodeBlockContent(code: String, language: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(
            text = code.trimEnd(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlockQuoteBlock(
    quote: BlockQuote,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    onTaskToggle: ((Int, Boolean) -> Unit)?,
) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(IntrinsicSize.Min)
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            var child = quote.firstChild
            while (child != null) {
                MarkdownBlock(child, onLinkClick, onTagClick, onTaskToggle)
                child = child.next
            }
        }
    }
}

@Composable
private fun BulletListBlock(
    list: BulletList,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    onTaskToggle: ((Int, Boolean) -> Unit)?,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        var child = list.firstChild
        while (child != null) {
            if (child is ListItem) {
                ListRow("•", child, onLinkClick, onTagClick, onTaskToggle)
            }
            child = child.next
        }
    }
}

@Composable
private fun OrderedListBlock(
    list: OrderedList,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    onTaskToggle: ((Int, Boolean) -> Unit)?,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        var index = list.startNumber
        var child = list.firstChild
        while (child != null) {
            if (child is ListItem) {
                ListRow("$index.", child, onLinkClick, onTagClick, onTaskToggle)
                index++
            }
            child = child.next
        }
    }
}

@Composable
private fun ListRow(
    marker: String,
    item: ListItem,
    onLinkClick: ((String) -> Unit)?,
    onTagClick: ((String) -> Unit)?,
    onTaskToggle: ((Int, Boolean) -> Unit)?,
) {
    val firstChild = item.firstChild
    val isTask = firstChild is Paragraph &&
        firstChild.firstChild is TaskListItemMarker

    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        if (isTask) {
            val marker = firstChild.firstChild as TaskListItemMarker
            Checkbox(
                checked = marker.isChecked,
                onCheckedChange = { checked -> onTaskToggle?.invoke(item.hashCode(), checked) },
                modifier = Modifier.padding(end = 8.dp),
            )
            Column(modifier = Modifier.padding(start = 4.dp)) {
                var sub = firstChild.next
                while (sub != null) {
                    MarkdownBlock(sub, onLinkClick, onTagClick, onTaskToggle)
                    sub = sub.next
                }
            }
        } else {
            Text(
                text = marker,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Column(modifier = Modifier.padding(start = 4.dp)) {
                var sub = firstChild
                while (sub != null) {
                    MarkdownBlock(sub, onLinkClick, onTagClick, onTaskToggle)
                    sub = sub.next
                }
            }
        }
    }
}

