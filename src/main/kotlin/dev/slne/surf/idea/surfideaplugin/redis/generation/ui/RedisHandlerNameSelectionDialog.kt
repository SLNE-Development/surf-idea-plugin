package dev.slne.surf.idea.surfideaplugin.redis.generation.ui

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import org.jetbrains.kotlin.name.Name
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTextField

class RedisHandlerNameSelectionDialog(
    project: Project,
    parentComponent: Component,
    private val targetDisplayName: String,
    private val targetClassName: String,
    defaultHandlerName: String,
    dialogTitle: String,
) : DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {

    private val graph = PropertyGraph("RedisHandlerNameSelectionDialog")
    private val handlerNameProperty = graph.property(defaultHandlerName)

    private lateinit var handlerNameField: JTextField

    val chosenName: String
        get() = handlerNameProperty.get().trim()


    init {
        title = dialogTitle
        init()
        initValidation()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("$targetDisplayName:") {
            label(targetClassName)
                .align(AlignX.FILL)
        }

        row("Handler name:") {
            handlerNameField = textField()
                .bindText(handlerNameProperty)
                .columns(COLUMNS_LARGE)
                .focused()
                .component
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return handlerNameField
    }

    override fun doValidate(): ValidationInfo? {
        val handlerName = chosenName

        if (handlerName.isBlank()) {
            return ValidationInfo(
                "Handler name must not be empty.",
                handlerNameField,
            )
        }

        if (!Name.isValidIdentifier(handlerName)) {
            return ValidationInfo(
                "Handler name must be a valid Kotlin identifier.",
                handlerNameField,
            )
        }

        return null
    }
}