package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtFile

abstract class FacetAwareAbstractKotlinInspection: AbstractKotlinInspection() {
    abstract fun requiredFacetType(): FacetTypeId<*>

    override fun isAvailableForFile(file: PsiFile): Boolean {
        if (file !is KtFile) return false
        val module = file.module ?: return false
        return FacetManager.getInstance(module).getFacetByType(requiredFacetType()) != null
    }
}