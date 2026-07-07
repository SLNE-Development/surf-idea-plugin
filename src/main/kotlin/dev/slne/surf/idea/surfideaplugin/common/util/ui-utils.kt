package dev.slne.surf.idea.surfideaplugin.common.util

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration

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
