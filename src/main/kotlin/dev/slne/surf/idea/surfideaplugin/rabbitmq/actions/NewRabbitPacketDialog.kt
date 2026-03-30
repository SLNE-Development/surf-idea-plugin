package dev.slne.surf.idea.surfideaplugin.rabbitmq.actions

import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.ui.dsl.builder.*
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import javax.swing.JComponent

class NewRabbitPacketDialog(
    private val project: Project,
    private val module: Module,
    private val packetType: PacketType,
    private val packetName: String
) : DialogWrapper(project, true) {

    enum class PacketType { REQUEST, RESPONSE }

    private val propertyGraph = PropertyGraph()

    private val createNewResponseProperty = propertyGraph.property(true)
    private val responsePacketFqnProperty = propertyGraph.property("")
    private val responsePacketSimpleNameProperty = propertyGraph.property("")
    private val newResponseNameProperty = propertyGraph.property(
        "${packetName.removeSuffix("Packet").removeSuffix("Request")}ResponsePacket"
    )
    private val generateDataClassProperty = propertyGraph.property(true)
    private val generateResponseDataClassProperty = propertyGraph.property(true)

    var createNewResponse: Boolean by createNewResponseProperty
    var responsePacketFqn: String by responsePacketFqnProperty
    var responsePacketSimpleName: String by responsePacketSimpleNameProperty
    var newResponseName: String by newResponseNameProperty
    var isGenerateDataClass: Boolean by generateDataClassProperty
    var isGenerateResponseDataClass: Boolean by generateResponseDataClassProperty

    init {
        title = when (packetType) {
            PacketType.REQUEST -> "New Rabbit Request Packet"
            PacketType.RESPONSE -> "New Rabbit Response Packet"
        }

        createNewResponseProperty.afterChange { value ->
            println("createNewResponseProperty changed to $value")
        }

        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Packet name:") {
            label(packetName).bold()
        }

        separator()
        row {
            checkBox("Generate data class")
                .bindSelected(generateDataClassProperty)
        }

        if (packetType == PacketType.REQUEST) {
            group("Response Packet") {
                buttonsGroup {
                    row {
                        radioButton("Select existing response packet")
                    }
                    indent {
                        row("Response packet:") {
                            textField()
                                .bindText(responsePacketSimpleNameProperty)
                                .columns(COLUMNS_MEDIUM)
                            button("Browse...") { browseResponsePacket() }
                        }
                    }.enabledIf(createNewResponseProperty.not())

                    row {
                        radioButton("Create new response packet")
                            .bindSelected(createNewResponseProperty)
                    }
                    indent {
                        row("New response name:") {
                            textField()
                                .bindText(newResponseNameProperty)
                                .columns(COLUMNS_MEDIUM)
                        }
                        row {
                            checkBox("Generate data class")
                                .bindSelected(generateResponseDataClassProperty)
                        }
                    }.enabledIf(createNewResponseProperty)
                }
            }
        }
    }

    private fun browseResponsePacket() {
        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                "Select Response Packet",
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false),
                { aClass ->
                    aClass.isConcreteClass() &&
                            InheritanceUtil.isInheritor(aClass, SurfRabbitClassNames.RABBIT_RESPONSE_PACKET_CLASS)
                },
                null
            )
        chooser.showDialog()
        val selected = chooser.selected ?: return
        responsePacketFqn = selected.qualifiedName ?: return
        responsePacketSimpleName = selected.name ?: return
    }

    override fun doValidate(): ValidationInfo? {
        if (packetType == PacketType.REQUEST) {
            if (!createNewResponse && responsePacketSimpleName.isBlank()) {
                return ValidationInfo("Please select or create a response packet")
            }
            if (createNewResponse && newResponseName.isBlank()) {
                return ValidationInfo("Please enter a name for the new response packet")
            }
        }
        return null
    }
}