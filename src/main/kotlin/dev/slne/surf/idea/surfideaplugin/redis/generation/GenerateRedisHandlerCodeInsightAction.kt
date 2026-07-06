package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class GenerateRedisHandlerCodeInsightAction(kind: RedisHandlerGenerationKind) : CodeInsightAction() {

    private val handler = GenerateRedisHandlerAction(kind)

    override fun getHandler(): CodeInsightActionHandler {
        return handler
    }

    override fun isValidForFile(
        project: Project,
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        return project.redisHandlerGenerationService().isAvailable(editor, file)
    }
}