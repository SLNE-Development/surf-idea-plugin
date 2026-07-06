package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service.velocityGenerationService

class GenerateVelocityEventListenerCodeInsightAction : CodeInsightAction() {
    private val handler = GenerateVelocityEventListenerAction()

    override fun getHandler(): CodeInsightActionHandler = handler

    override fun isValidForFile(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
        return project.velocityGenerationService().isAvailable(editor, psiFile)
    }
}