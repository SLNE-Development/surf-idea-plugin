package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile

internal class DecompiledJarHighlightInfoFilter : HighlightInfoFilter {

    override fun accept(highlightInfo: HighlightInfo, psiFile: PsiFile?): Boolean {
        val file = psiFile?.viewProvider?.virtualFile ?: return true
        if (!file.isDecompiledJarClass) return true
        return highlightInfo.severity <= HighlightSeverity.INFORMATION
    }
}
