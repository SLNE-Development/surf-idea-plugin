package dev.slne.surf.idea.surfideaplugin.common.inspection

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Base class for Surf inspections built on the K2 applicable-inspection API.
 *
 * The inspection only runs in files whose module has all [requiredLibraries] on the
 * classpath. The check happens once per file; subclasses implement the usual
 * [isApplicableByPsi]/[prepareContext] pair without worrying about library detection.
 */
abstract class SurfApplicableInspection<ELEMENT : KtElement, CONTEXT : Any>(
    private vararg val requiredLibraries: SurfLibraryMarker,
) : KotlinApplicableInspectionBase<ELEMENT, CONTEXT>() {

    final override fun isAvailableForFile(file: PsiFile): Boolean {
        if (file !is KtFile) return false

        val module = file.module ?: return false
        return isAvailableForModule(module)
    }

    protected open fun isAvailableForModule(module: Module): Boolean {
        return SurfLibraryDetector.hasAllLibraries(module, requiredLibraries)
    }
}
