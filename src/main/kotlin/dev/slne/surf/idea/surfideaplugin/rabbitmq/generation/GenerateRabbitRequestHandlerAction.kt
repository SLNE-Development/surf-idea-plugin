package dev.slne.surf.idea.surfideaplugin.rabbitmq.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import dev.slne.surf.idea.surfideaplugin.rabbitmq.generation.ui.RabbitRequestHandlerNameSelectionDialog
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

class GenerateRabbitRequestHandlerAction : CodeInsightActionHandler {

    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        val module = psiFile.module ?: return
        if (!SurfLibraryDetector.hasSurfRabbitMqServer(module)) return
        val caretElement = psiFile.findElementAt(editor.caretModel.offset) ?: return
        val targetClass = caretElement.getParentOfType<KtClassOrObject>(strict = false) ?: return

        if (!EditorModificationUtil.requestWriting(editor)) {
            return
        }

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                RefactoringBundle.message("choose.destination.class"),
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false),
                RabbitRequestClassFilter,
                null
            )

        chooser.showDialog()
        val chosenClass = chooser.selected ?: return
        val shortChosenClassName = chosenClass.nameIdentifier?.text ?: return
        val fqChosenClassName = chosenClass.qualifiedName ?: return
        val defaultHandlerName = "handle" + shortChosenClassName.removeSuffix("Packet").removeSuffix("Request")

        val dialog = RabbitRequestHandlerNameSelectionDialog(
            editor,
            "RabbitMQ Request Class",
            shortChosenClassName,
            defaultHandlerName
        )
        if (!dialog.showAndGet()) return

        currentThreadCoroutineScope().launch {
            generateRequestHandler(
                project = project,
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.chosenName,
                requestClassName = fqChosenClassName
            )
        }
    }

    private suspend fun generateRequestHandler(
        project: Project,
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        requestClassName: String
    ) {
        val annotationFqn = SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION

        val functionText = """
            @$annotationFqn
            fun $handlerName(${SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME}: $requestClassName) {
            }
        """.trimIndent()

        writeCommandAction(project, "Generate RabbitMQ Event Handler") {
            val factory = KtPsiFactory(project)
            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            val inserted = insertMembersAfterAndReformat(editor, targetClass, listOf(prototype), anchor)
            inserted.firstOrNull()?.let {
                ShortenReferencesFacility.getInstance().shorten(it)
            }
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    private object RabbitRequestClassFilter : ClassFilter {
        override fun isAccepted(aClass: PsiClass): Boolean {
            if (!aClass.isConcreteClass()) return false
            return InheritanceUtil.isInheritor(aClass, SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS)
        }
    }
}