package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GenerateVelocityEventListenerCodeInsightAction : CodeInsightAction() {
    private val handler = GenerateVelocityEventListenerAction()

    override fun getHandler(): CodeInsightActionHandler = handler

    override fun isValidForFile(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
        val module = psiFile.module ?: return false
        if (!SurfLibraryDetector.hasSurfApiVelocity(module)) return false

        val caretElement = psiFile.findElementAt(editor.caretModel.offset) ?: return false
        return caretElement.getParentOfType<KtClassOrObject>(strict = false) != null
    }
}