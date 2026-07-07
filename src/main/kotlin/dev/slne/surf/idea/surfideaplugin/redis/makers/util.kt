package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens

fun PsiElement.isKtIdentifier(): Boolean {
    return this is LeafPsiElement && elementType == KtTokens.IDENTIFIER
}
