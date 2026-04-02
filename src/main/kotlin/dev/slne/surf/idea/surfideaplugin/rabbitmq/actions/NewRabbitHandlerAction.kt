package dev.slne.surf.idea.surfideaplugin.rabbitmq.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateInDirectoryActionBase
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.WriteActionAware
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.util.getDestructuringParamNames
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.asJava.classes.KtLightClass

class NewRabbitHandlerAction : CreateInDirectoryActionBase(
    "Rabbit Handler",
    "Creates a new Rabbit Handler for a RabbitRequestPacket",
    AllIcons.Providers.RabbitMQ
), WriteActionAware {
    companion object {
        const val TEMPLATE_NAME = "Rabbit Handler Class"
        private const val CARET_MARKER = "// CARET_POSITION"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val view = e.dataContext.getData(LangDataKeys.IDE_VIEW) ?: return
        val directory = view.orChooseDirectory ?: return

        val baseClass = JavaPsiFacade.getInstance(project)
            .findClass(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS, GlobalSearchScope.allScope(project))
            ?: return

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createInheritanceClassChooser(
                "Choose Request Packet",
                GlobalSearchScope.allScope(project),
                baseClass,
                null,
                PsiClass::isConcreteClass
            )

        chooser.showDialog()
        val selectedClass = chooser.selected ?: return
        val requestSimpleName = selectedClass.name ?: return
        val requestFqn = selectedClass.qualifiedName ?: return

        val handlerClassName = requestSimpleName
            .removeSuffix("Packet")
            .removeSuffix("Request")
            .plus("RequestHandler")

        val handlerMethodName = "handle" + requestSimpleName.removeSuffix("Packet")

        if (directory.findFile("$handlerClassName.kt") != null) {
            Messages.showErrorDialog(
                project,
                "$handlerClassName.kt already exists in this directory.",
                "Rabbit Handler"
            )
            return
        }

        val templateManager = FileTemplateManager.getInstance(project)
        val template = templateManager.getInternalTemplate(TEMPLATE_NAME)

        val props = templateManager.defaultProperties.apply {
            setProperty("HANDLER_CLASS_NAME", handlerClassName)
            setProperty("HANDLER_METHOD_NAME", handlerMethodName)
            setProperty("REQUEST_CLASS_NAME", requestSimpleName)
            setProperty("REQUEST_IMPORT", requestFqn)
            setProperty("CARET_PLACEHOLDER", CARET_MARKER)
        }

        currentThreadCoroutineScope().launch {
            val destructingParams = (selectedClass as? KtLightClass)?.getDestructuringParamNames()
                ?.takeUnless { it.size > 10 }
                ?.joinToString(", ")

            writeCommandAction(project, "Create Rabbit Handler") {
                val element = FileTemplateUtil.createFromTemplate(
                    template,
                    "$handlerClassName.kt",
                    props,
                    directory
                )

                val virtualFile = element.containingFile?.virtualFile ?: return@writeCommandAction
                val editors = FileEditorManager.getInstance(project).openFile(virtualFile, true)

                val textEditor = editors.filterIsInstance<TextEditor>().firstOrNull() ?: return@writeCommandAction
                val editor = textEditor.editor
                val document = editor.document
                val text = document.text
                val markerOffset = text.indexOf(CARET_MARKER)

                if (markerOffset >= 0) {
                    document.deleteString(markerOffset, markerOffset + CARET_MARKER.length)

                    if (destructingParams != null) {
                        val destructingParamsText =
                            "val ($destructingParams) = ${SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME}\n        // Handle request here"

                        document.insertString(markerOffset, destructingParamsText)
                        editor.caretModel.moveToOffset(
                            markerOffset + destructingParamsText.length
                        )
                    } else {
                        editor.caretModel.moveToOffset(markerOffset)
                    }
                }
            }
        }
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false
        val module = dataContext.getData(PlatformCoreDataKeys.MODULE) ?: return false
        return SurfLibraryDetector.hasSurfRabbitMqServer(module)
    }

    override fun startInWriteAction(): Boolean = false
}