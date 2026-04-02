package dev.slne.surf.idea.surfideaplugin.rabbitmq.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.util.findInsertOffset
import dev.slne.surf.idea.surfideaplugin.common.util.getDestructuringParamNames
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

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
        val defaultHandlerName = "handle" + shortChosenClassName
            .removeSuffix("Packet")
            .removeSuffix("Request")

        currentThreadCoroutineScope().launch {
            generateRequestHandler(
                project = project,
                editor = editor,
                targetClass = targetClass,
                handlerName = defaultHandlerName,
                requestClass = chosenClass,
                requestClassName = fqChosenClassName
            )
        }
    }

    private suspend fun generateRequestHandler(
        project: Project,
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        requestClass: PsiClass,
        requestClassName: String
    ) {
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        template.isToReformat = true
        template.isToShortenLongNames = true

        template.addTextSegment("@${SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION}\n")
        // optionally make suspend
        template.addVariable("SUSPEND", ConstantNode("suspend "), true)
        template.addTextSegment("fun ")
        template.addVariable("HANDLER_NAME", ConstantNode(handlerName), true)
        template.addTextSegment("(${SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME}: $requestClassName) {\n")

        if (requestClass is KtLightClass) {
            requestClass.getDestructuringParamNames()?.takeUnless { it.size > 10 }?.let { params ->
                val paramList = params.joinToString(", ")
                template.addTextSegment(
                    "val ($paramList) = ${SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME}\n"
                )
            }
        }

        template.addEndVariable()
        template.addTextSegment("\n}")

        writeCommandAction(project, "Generate RabbitMQ Request Handler") {
            val insertOffset = findInsertOffset(editor, targetClass)

            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
            editor.document.insertString(insertOffset, "\n")
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
            editor.caretModel.moveToOffset(insertOffset + 2)

            templateManager.startTemplate(editor, template)
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