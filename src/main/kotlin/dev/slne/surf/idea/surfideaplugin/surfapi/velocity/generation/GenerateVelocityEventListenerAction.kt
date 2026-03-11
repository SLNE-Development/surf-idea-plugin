package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service.velocityGenerationService
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.ui.VelocityEventListenerDialog
import kotlinx.coroutines.launch
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
        val module = psiFile.module ?: return
        val caretElement = psiFile.findElementAt(editor.caretModel.offset) ?: return
        val targetClass = caretElement.getParentOfType<KtClassOrObject>(strict = false) ?: return

        if (!EditorModificationUtil.requestWriting(editor)) return

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                RefactoringBundle.message("choose.destination.class"),
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false),
                null,
                null
            )

        chooser.showDialog()
        val chosenClass = chooser.selected ?: return
        val shortChosenClassName = chosenClass.nameIdentifier?.text ?: return
        val fqChosenClassName = chosenClass.qualifiedName ?: return
        val defaultHandlerName = "on" + shortChosenClassName.removeSuffix("Event")

        val dialog = VelocityEventListenerDialog(
            project = project,
            eventClassName = shortChosenClassName,
            defaultHandlerName = defaultHandlerName
        )
        if (!dialog.showAndGet()) return

        currentThreadCoroutineScope().launch {
            project.velocityGenerationService().generateEventListener(
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.handlerName,
                eventClassName = fqChosenClassName,
                priority = dialog.selectedPriority,
                suspendHandler = dialog.suspendHandler
            )
        }
    }
}