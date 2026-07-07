package dev.slne.surf.idea.surfideaplugin.common.inspection

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtFile

abstract class SurfKotlinInspection(
    private vararg val requiredLibraries: SurfLibraryMarker
): AbstractKotlinInspection() {

    @MustBeInvokedByOverriders
    override fun isAvailableForFile(file: PsiFile): Boolean {
        if (file !is KtFile) return false

        val module = file.module ?: return false
        return isAvailableForModule(module)
    }

    protected open fun isAvailableForModule(module: Module): Boolean {
        if (requiredLibraries.isEmpty()) return true
        return SurfLibraryDetector.hasAllLibraries(module, requiredLibraries)
    }
}