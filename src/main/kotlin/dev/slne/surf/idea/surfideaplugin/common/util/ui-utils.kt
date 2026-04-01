package dev.slne.surf.idea.surfideaplugin.common.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.editor.Editor
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.typography
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration

@Composable
fun LabeledRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.3f),
            style = JewelTheme.typography.labelTextStyle
        )
        Row(modifier = Modifier.weight(0.7f)) {
            content()
        }
    }
}

fun findInsertOffset(editor: Editor, targetClass: KtClassOrObject): Int {
    val caretOffset = editor.caretModel.offset
    val body = targetClass.body
        ?: return targetClass.textRange.endOffset

    val declarations = body.declarations
    if (declarations.isEmpty()) {
        return body.lBrace?.textRange?.endOffset
            ?: targetClass.textRange.endOffset
    }

    var anchor: KtDeclaration? = null
    for (declaration in declarations) {
        if (declaration.textRange.startOffset <= caretOffset) {
            anchor = declaration
        } else {
            break
        }
    }

    return anchor?.textRange?.endOffset
        ?: body.lBrace?.textRange?.endOffset
        ?: targetClass.textRange.endOffset
}