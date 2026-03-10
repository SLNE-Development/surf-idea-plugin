package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.PaperClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.service.paperGenerationService
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.ui.PaperEventListenerDialog
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GeneratePaperEventListenerAction : CodeInsightActionHandler {
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
                PaperEventClassFilter,
                null
            )

        chooser.showDialog()
        val chosenClass = chooser.selected ?: return
        val shortChosenClassName = chosenClass.nameIdentifier?.text ?: return
        val fqChosenClassName = chosenClass.qualifiedName ?: return
        val defaultHandlerName = "on" + shortChosenClassName.removeSuffix("Event")

        val dialog = PaperEventListenerDialog(
            project = project,
            eventClassName = shortChosenClassName,
            defaultHandlerName = defaultHandlerName
        )
        if (!dialog.showAndGet()) return

        currentThreadCoroutineScope().launch {
            project.paperGenerationService().generateEventListener(
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.handlerName,
                eventClassName = fqChosenClassName,
                priority = dialog.priority,
                ignoreCancelled = dialog.ignoreCancelled
            )
        }
    }

    private object PaperEventClassFilter : ClassFilter {
        override fun isAccepted(aClass: PsiClass): Boolean {
            if (!InheritanceUtil.isInheritor(aClass, PaperClassNames.EVENT_CLASS)) return false

            val hasHandlerList = aClass.methods.any { method ->
                method.name == "getHandlerList"
                        && method.hasModifierProperty("static")
                        && method.parameterList.parametersCount == 0
            }

            return hasHandlerList
        }
    }
}