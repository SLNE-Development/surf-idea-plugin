package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service.VelocityGenerationService
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service.VelocityGenerationService.VelocityEventListenerGenerationConfig
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service.velocityGenerationService
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.ui.VelocityEventListenerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GenerateVelocityEventListenerAction : CodeInsightActionHandler {
    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        currentThreadCoroutineScope().launch(ModalityState.current().asContextElement()) {
            val service = project.velocityGenerationService()

            val target = service.prepareGenerationTarget(
                editor = editor,
                psiFile = psiFile,
            ) ?: return@launch

            val selectedEventClass = withContext(Dispatchers.EDT) {
                service.chooseEventClass(target.module)
            } ?: return@launch

            val defaultHandlerName = service.createDefaultHandlerName(
                eventClassName = selectedEventClass.shortName,
                existingFunctionNames = target.existingFunctionNames,
            )

            val config = withContext(Dispatchers.EDT) {
                val dialog = VelocityEventListenerDialog(
                    project = project,
                    parentComponent = editor.component,
                    eventClassName = selectedEventClass.shortName,
                    defaultHandlerName = defaultHandlerName,
                    existingHandlerNames = target.existingFunctionNames,
                )

                if (!dialog.showAndGet()) {
                    return@withContext null
                }

                VelocityEventListenerGenerationConfig(
                    handlerName = dialog.handlerName,
                    eventClassFqName = selectedEventClass.qualifiedName,
                    priority = dialog.selectedOrder,
                    suspendHandler = dialog.suspendHandler,
                )
            } ?: return@launch

            val canWrite = withContext(Dispatchers.EDT) {
                EditorModificationUtil.requestWriting(editor)
            }

            if (!canWrite) {
                return@launch
            }

            service.generateEventListener(
                editor = editor,
                targetClassPointer = target.targetClassPointer,
                config = config,
            )
        }
    }
}