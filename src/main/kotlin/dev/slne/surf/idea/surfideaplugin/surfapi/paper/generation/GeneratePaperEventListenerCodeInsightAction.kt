package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.service.paperGenerationService

class GeneratePaperEventListenerCodeInsightAction : CodeInsightAction() {
    private val handler = GeneratePaperEventListenerAction()

    override fun getHandler(): CodeInsightActionHandler = handler

    override fun isValidForFile(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
        return project.paperGenerationService().isAvailable(editor, psiFile)
    }
}