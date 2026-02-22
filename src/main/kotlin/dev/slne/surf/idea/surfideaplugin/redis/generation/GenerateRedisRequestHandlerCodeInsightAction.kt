package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GenerateRedisRequestHandlerCodeInsightAction: CodeInsightAction() {
    private val handler = GenerateRedisRequestHandlerAction()

    override fun getHandler(): CodeInsightActionHandler = handler

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(file)) return false
        val caretElement = file.findElementAt(editor.caretModel.offset) ?: return false
        return caretElement.getParentOfType<KtClassOrObject>(strict = false) != null
    }
}