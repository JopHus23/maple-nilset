package com.mapherdf.maplenilset.ui.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.ins.InsExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.ext.footnotes.FootnotesExtension

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    onTaskToggle: ((Int, Boolean) -> Unit)? = null,
) {
    val parser = remember {
        Parser.builder()
            .extensions(
                listOf(
                    TablesExtension.create(),
                    StrikethroughExtension.create(),
                    InsExtension.create(),
                    TaskListItemsExtension.create(),
                    FootnotesExtension.create(),
                ),
            )
            .build()
    }
    val document = remember(markdown) { parser.parse(markdown) }

    Column(modifier = modifier.fillMaxWidth()) {
        var child = document.firstChild
        while (child != null) {
            MarkdownBlock(
                node = child,
                onLinkClick = onLinkClick,
                onTagClick = onTagClick,
                onTaskToggle = onTaskToggle,
            )
            child = child.next
        }
    }
}

