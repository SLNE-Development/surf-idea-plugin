package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import org.jetbrains.kotlin.lexer.KtTokens

fun PsiElement.isKtIdentifier(): Boolean {
    return this is LeafPsiElement && elementType == KtTokens.IDENTIFIER
}

fun Module.hasSurfRedis(cache: MutableMap<Module, Boolean>): Boolean {
    return cache.computeIfAbsent(this) {
        SurfLibraryDetector.hasLibrary(
            this,
            SurfLibraryMarker.SURF_REDIS_API,
        )
    }
}