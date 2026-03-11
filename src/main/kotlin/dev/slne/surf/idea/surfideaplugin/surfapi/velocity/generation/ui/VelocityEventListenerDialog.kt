package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBDimension
import dev.slne.surf.idea.surfideaplugin.common.util.LabeledRow
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.typography
import javax.swing.JComponent

class VelocityEventListenerDialog(
    project: Project,
    private val eventClassName: String,
    defaultHandlerName: String
) : DialogWrapper(project) {

    private val handlerNameState = TextFieldState(defaultHandlerName)
    private val priorityState = TextFieldState("0")

    val handlerName: String get() = handlerNameState.text.toString()
    val selectedPriority: Int get() = priorityState.text.toString().toIntOrNull() ?: 0
    var suspendHandler: Boolean by mutableStateOf(false)
        private set

    init {
        title = "Generate Velocity Event Listener"
        init()
    }

    override fun createCenterPanel(): JComponent {
        enableNewSwingCompositing()
        return JewelComposePanel {
            VelocityEventListenerContent()
        }.apply {
            minimumSize = JBDimension(400, 150)
            preferredSize = JBDimension(450, 160)
        }
    }

    @Composable
    private fun VelocityEventListenerContent() {
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
                LabeledRow("Priority:") {
                    IntFieldWithSuggestions(
                        state = priorityState,
                        presets = priorityPresets,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            CheckboxRow(
                text = "suspend Handler",
                checked = suspendHandler,
                onCheckedChange = { suspendHandler = it }
            )
        }
    }

    @Composable
    private fun IntFieldWithSuggestions(
        state: TextFieldState,
        presets: List<PriorityPreset>,
        modifier: Modifier = Modifier
    ) {
        var showSuggestions by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current

        Box(modifier = modifier) {
            TextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        showSuggestions = focusState.isFocused
                    },
                placeholder = { Text("e.g. 100") }
            )

            if (showSuggestions) {
                Popup(
                    onDismissRequest = { showSuggestions = false }
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .background(
                                JewelTheme.globalColors.panelBackground,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                JewelTheme.globalColors.borders.normal,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)
                    ) {
                        presets.forEach { preset ->
                            Text(
                                text = preset.toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        state.edit {
                                            replace(0, length, preset.value.toString())
                                        }
                                        showSuggestions = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? = null

    data class PriorityPreset(val label: String, val value: Int) {
        override fun toString() = "$label ($value)"
    }

    val priorityPresets = listOf(
        PriorityPreset("FIRST", Short.MAX_VALUE - 1),
        PriorityPreset("EARLY", Short.MAX_VALUE / 2),
        PriorityPreset("NORMAL", 0),
        PriorityPreset("LATE", Short.MIN_VALUE / 2),
        PriorityPreset("LAST", Short.MIN_VALUE + 1),
    )
}