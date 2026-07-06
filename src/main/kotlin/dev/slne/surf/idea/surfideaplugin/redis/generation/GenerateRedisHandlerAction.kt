package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.squareup.kotlinpoet.ClassName
import dev.slne.surf.idea.surfideaplugin.redis.generation.ui.RedisHandlerNameSelectionDialog
import org.jetbrains.kotlin.idea.base.util.module

open class GenerateRedisHandlerAction(
    private val kind: RedisHandlerGenerationKind
) : CodeInsightActionHandler {
    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        val generationService = project.redisHandlerGenerationService()

        if (!generationService.isAvailable(editor, psiFile)) {
            return
        }

        val module = psiFile.module ?: return
        val targetClass = generationService.findTargetClass(editor, psiFile) ?: return

        val selectedClass = generationService.chooseRedisClass(
            module = module,
            kind = kind,
        ) ?: return

        val selectedClassName = selectedClass.name ?: return
        val selectedClassFqn = ClassName.bestGuess(selectedClass.qualifiedName ?: return)

        val defaultHandlerName = kind.createDefaultHandlerName(
            selectedClassName = selectedClassName,
            existingFunctionNames = generationService.existingFunctionNames(targetClass),
        )

        val dialog = RedisHandlerNameSelectionDialog(
            project = project,
            parentComponent = editor.component,
            targetDisplayName = kind.targetDisplayName,
            targetClassName = selectedClassName,
            defaultHandlerName = defaultHandlerName,
            dialogTitle = kind.dialogTitle,
        )

        if (!dialog.showAndGet()) {
            return
        }

        generationService.generateHandler(
            editor = editor,
            targetClass = targetClass,
            kind = kind,
            handlerName = dialog.chosenName,
            selectedClass = selectedClassFqn,
        )
    }

    override fun startInWriteAction(): Boolean {
        return false
    }
}