package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.util.PaperEventListenerPriorities
import org.jetbrains.kotlin.name.Name
import java.awt.Component
import javax.swing.JComponent

class PaperEventListenerDialog(
    project: Project,
    parentComponent: Component,
    private val eventClassName: String,
    defaultHandlerName: String,
    private val existingHandlerNames: Set<String>,
) : DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {

    private val handlerNameField = JBTextField(defaultHandlerName)
    private val priorityComboBox = ComboBox(
        PaperEventListenerPriorities.entries.toTypedArray(),
    ).apply {
        selectedItem = PaperEventListenerPriorities.NORMAL
    }

    private val ignoreCancelledCheckBox = JBCheckBox(
        "Ignore cancelled events",
        true,
    )

    val handlerName: String
        get() = handlerNameField.text.trim()

    val priority: PaperEventListenerPriorities
        get() = priorityComboBox.selectedItem as PaperEventListenerPriorities

    val ignoreCancelled: Boolean
        get() = ignoreCancelledCheckBox.isSelected

    init {
        title = "Generate Paper Event Listener"
        init()
        initValidation()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Event class:") {
            label(eventClassName)
                .align(AlignX.FILL)
        }

        row("Handler name:") {
            cell(handlerNameField)
                .columns(COLUMNS_LARGE)
                .focused()
                .align(AlignX.FILL)
        }

        row("Priority:") {
            cell(priorityComboBox)
                .align(AlignX.FILL)
        }

        row {
            cell(ignoreCancelledCheckBox)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return handlerNameField
    }

    override fun doValidate(): ValidationInfo? {
        val name = handlerName

        if (name.isBlank()) {
            return ValidationInfo(
                "Handler name must not be empty.",
                handlerNameField,
            )
        }

        if (!Name.isValidIdentifier(name)) {
            return ValidationInfo(
                "Handler name must be a valid Kotlin function name.",
                handlerNameField,
            )
        }

        if (name in existingHandlerNames) {
            return ValidationInfo(
                "A function named '$name' already exists in this class.",
                handlerNameField,
            )
        }

        return null
    }
}