package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.service.paperGenerationService
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.ui.PaperEventListenerDialog
import dev.slne.surf.idea.surfideaplugin.util.className
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.base.util.module

class GeneratePaperEventListenerAction : CodeInsightActionHandler {
    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        val service = project.paperGenerationService()

        if (!service.isAvailable(editor, psiFile)) {
            return
        }

        val module = psiFile.module ?: return
        val targetClass = service.findTargetClass(editor, psiFile) ?: return

        val chosenEventClass = service.chooseEventClass(module) ?: return
        val eventClassShortName = chosenEventClass.name ?: return
        val eventClassName = chosenEventClass.className() ?: return

        val existingFunctionNames = service.existingFunctionNames(targetClass)

        val defaultHandlerName = service.createDefaultHandlerName(
            eventClassShortName = eventClassShortName,
            existingFunctionNames = existingFunctionNames,
        )

        val dialog = PaperEventListenerDialog(
            project = project,
            parentComponent = editor.component,
            eventClassName = eventClassShortName,
            defaultHandlerName = defaultHandlerName,
            existingHandlerNames = existingFunctionNames,
        )

        if (!dialog.showAndGet()) {
            return
        }

        currentThreadCoroutineScope().launch {
            service.generateEventListener(
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.handlerName,
                eventClassName = eventClassName,
                priority = dialog.priority,
                ignoreCancelled = dialog.ignoreCancelled,
            )
        }
    }
}