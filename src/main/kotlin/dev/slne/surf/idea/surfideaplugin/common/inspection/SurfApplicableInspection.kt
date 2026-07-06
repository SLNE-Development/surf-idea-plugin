package dev.slne.surf.idea.surfideaplugin.common.inspection

import com.intellij.openapi.module.Module
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.psi.KtElement

abstract class SurfApplicableInspection<ELEMENT : KtElement, CONTEXT : Any>(
    private vararg val requiredLibraries: SurfLibraryMarker,
) : KotlinApplicableInspectionBase<ELEMENT, CONTEXT>() {

    final override fun isApplicableByPsi(element: ELEMENT): Boolean {
        val module = element.module ?: return false

        if (!isAvailableForModule(module)) {
            return false
        }

        return isSurfApplicableByPsi(element)
    }

    protected open fun isAvailableForModule(module: Module): Boolean {
        if (requiredLibraries.isEmpty()) return true
        return SurfLibraryDetector.hasAllLibraries(module, requiredLibraries)
    }


    protected abstract fun isSurfApplicableByPsi(element: ELEMENT): Boolean
}