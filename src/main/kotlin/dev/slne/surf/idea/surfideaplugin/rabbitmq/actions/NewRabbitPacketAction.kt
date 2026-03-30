package dev.slne.surf.idea.surfideaplugin.rabbitmq.actions

import com.intellij.facet.FacetManager
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.rabbitmq.facet.SurfRabbitFacetType


class NewRabbitPacketAction : CreateFileFromTemplateAction(), DumbAware {

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder
            .setTitle("New Rabbit Packet")
            .addKind("Request", AllIcons.Nodes.Class, RABBIT_REQUEST_TEMPLATE)
            .addKind("Response", AllIcons.Nodes.Class, RABBIT_RESPONSE_TEMPLATE)
    }

    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = "Create Rabbit Packet '$newName'"

    override fun createFileFromTemplate(
        name: String,
        template: FileTemplate,
        dir: PsiDirectory
    ): PsiFile? {
        val project = dir.project
        val module = ModuleUtilCore.findModuleForPsiElement(dir)
            ?: return createSimple(name, template, dir, emptyMap())

        val packetType = when (template.name) {
            RABBIT_REQUEST_TEMPLATE -> NewRabbitPacketDialog.PacketType.REQUEST
            RABBIT_RESPONSE_TEMPLATE -> NewRabbitPacketDialog.PacketType.RESPONSE
            else -> return createSimple(name, template, dir, emptyMap())
        }

        val dialog = NewRabbitPacketDialog(project, module, packetType, name)
        if (!dialog.showAndGet()) return null

        return when (packetType) {
            NewRabbitPacketDialog.PacketType.REQUEST -> {
                val responseSimpleName = if (dialog.createNewResponse) {
                    dialog.newResponseName
                } else {
                    dialog.responsePacketSimpleName
                }

                val responseImport = if (!dialog.createNewResponse && dialog.responsePacketFqn.isNotBlank()) {
                    dialog.responsePacketFqn
                } else {
                    "" // Same package or will be created
                }

                val classBody =
                    RabbitPacketCodeGenerator.generateRequestBody(name, dialog.isGenerateDataClass, responseSimpleName)
                val extraProps = mapOf(
                    "CLASS_BODY" to classBody,
                    "RESPONSE_IMPORT" to responseImport
                )

                val requestFile = createSimple(name, template, dir, extraProps)

                if (dialog.createNewResponse) {
                    createResponsePacketAlongside(
                        dialog.newResponseName,
                        dialog.isGenerateResponseDataClass,
                        dir,
                        project
                    )
                } else {
                    requestFile
                }
            }

            NewRabbitPacketDialog.PacketType.RESPONSE -> {
                val classBody = RabbitPacketCodeGenerator.generateResponseBody(name, dialog.isGenerateDataClass)
                val extraProps = mapOf("CLASS_BODY" to classBody)
                createSimple(name, template, dir, extraProps)
            }
        }
    }

    private fun createSimple(
        name: String,
        template: FileTemplate,
        dir: PsiDirectory,
        extraProps: Map<String, String>
    ): PsiFile? {
        val project = dir.project
        val templateProperties = FileTemplateManager.getInstance(project).defaultProperties
        extraProps.forEach { (k, v) -> templateProperties.setProperty(k, v) }

        return runWriteAction {
            CreateFileFromTemplateAction.createFileFromTemplate(
                name,
                template,
                dir,
                null,
                true,
                emptyMap(),
                templateProperties.map { (key, value) -> key.toString() to value.toString() }.toMap()
            )
        }
    }


    private fun createResponsePacketAlongside(
        responseName: String,
        dataClass: Boolean,
        dir: PsiDirectory,
        project: Project
    ): PsiFile? {
        val templateManager = FileTemplateManager.getInstance(project)
        val responseTemplate = templateManager.getInternalTemplate(RABBIT_RESPONSE_TEMPLATE)

        val classBody = RabbitPacketCodeGenerator.generateResponseBody(responseName, dataClass)
        val props = templateManager.defaultProperties
        props.setProperty("CLASS_BODY", classBody)

        return runWriteAction {
            CreateFileFromTemplateAction.createFileFromTemplate(
                responseName,
                responseTemplate,
                dir,
                null,
                true,
                emptyMap(),
                props.map { (key, value) -> key.toString() to value.toString() }.toMap()
            )
        }
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false
        val module = dataContext.getData(PlatformCoreDataKeys.MODULE) ?: return false
        return FacetManager.getInstance(module).getFacetByType(SurfRabbitFacetType.ID) != null
    }

    companion object {
        const val RABBIT_REQUEST_TEMPLATE = "Rabbit Request Packet"
        const val RABBIT_RESPONSE_TEMPLATE = "Rabbit Response Packet"
    }
}