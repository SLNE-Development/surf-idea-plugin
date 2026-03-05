package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBDimension
import dev.slne.surf.idea.surfideaplugin.common.util.LabeledRow
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.typography
import javax.swing.JComponent

class PaperEventListenerDialog(
    project: Project,
    private val eventClassName: String,
    defaultHandlerName: String
) : DialogWrapper(project) {

    private val handlerNameState = TextFieldState(defaultHandlerName)
    private var selectedPriorityIndex by mutableIntStateOf(2) // NORMAL
    private var ignoreCancelledState by mutableStateOf(false)

    val handlerName: String get() = handlerNameState.text.toString()
    val priority: String get() = PRIORITIES[selectedPriorityIndex]
    val ignoreCancelled: Boolean get() = ignoreCancelledState

    init {
        title = "Generate Paper Event Listener"
        init()
    }

    override fun createCenterPanel(): JComponent {
        enableNewSwingCompositing()
        return JewelComposePanel {
            PaperEventListenerContent()
        }.apply {
            minimumSize = JBDimension(400, 150)
            preferredSize = JBDimension(450, 160)
        }
    }

    @Composable
    private fun PaperEventListenerContent() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledRow("Event class:") {
                Text(eventClassName, style = JewelTheme.typography.labelTextStyle)
            }

            LabeledRow("Handler name:") {
                TextField(
                    state = handlerNameState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LabeledRow("Priority:") {
                ListComboBox(
                    items = PRIORITIES.toList(),
                    modifier = Modifier.fillMaxWidth(),
                    selectedIndex = selectedPriorityIndex,
                    onSelectedItemChange = { selectedPriorityIndex = it },
                )
            }

            CheckboxRow(
                text = "Ignore cancelled events",
                checked = ignoreCancelledState,
                onCheckedChange = { ignoreCancelledState = it }
            )
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? = null

    companion object {
        private val PRIORITIES = arrayOf(
            "LOWEST",
            "LOW",
            "NORMAL",
            "HIGH",
            "HIGHEST",
            "MONITOR"
        )
    }
}