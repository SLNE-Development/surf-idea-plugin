package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.name.Name
import java.awt.Component
import javax.swing.JComponent

class VelocityEventListenerDialog(
    project: Project,
    parentComponent: Component,
    private val eventClassName: String,
    defaultHandlerName: String,
    private val existingHandlerNames: Set<String>,
) : DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {

    private val handlerNameField = JBTextField(defaultHandlerName)

    private val orderSpinner = JBIntSpinner(
        VelocitySubscribeOrderPreset.NORMAL.value,
        Short.MIN_VALUE.toInt(),
        Short.MAX_VALUE.toInt(),
    )

    private val orderPresetComboBox = ComboBox(
        VelocitySubscribeOrderPreset.entries.toTypedArray()
    ).apply {
        selectedItem = VelocitySubscribeOrderPreset.NORMAL

        addActionListener {
            val preset = selectedItem as? VelocitySubscribeOrderPreset ?: return@addActionListener
            orderSpinner.value = preset.value
        }
    }

    private val suspendHandlerCheckBox = JBCheckBox(
        "Generate suspend handler",
        false,
    )

    val handlerName: String
        get() = handlerNameField.text.trim()

    val selectedOrder: Int
        get() = (orderSpinner.value as Number).toInt()

    val suspendHandler: Boolean
        get() = suspendHandlerCheckBox.isSelected


    init {
        title = "Generate Velocity Event Listener"
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

        row("Order preset:") {
            cell(orderPresetComboBox)
                .align(AlignX.FILL)
        }

        row("Order:") {
            cell(orderSpinner)
        }

        row {
            cell(suspendHandlerCheckBox)
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

    enum class VelocitySubscribeOrderPreset(
        val label: String,
        val value: Int,
    ) {
        FIRST("FIRST", Short.MAX_VALUE - 1),
        EARLY("EARLY", Short.MAX_VALUE / 2),
        NORMAL("NORMAL", 0),
        LATE("LATE", Short.MIN_VALUE / 2),
        LAST("LAST", Short.MIN_VALUE + 1);

        override fun toString(): String = buildString {
            append(label)
            append(" (")
            append(value)
            append(")")
        }
    }
}