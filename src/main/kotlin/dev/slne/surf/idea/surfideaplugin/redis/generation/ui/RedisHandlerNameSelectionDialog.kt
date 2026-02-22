package dev.slne.surf.idea.surfideaplugin.redis.generation.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class RedisHandlerNameSelectionDialog(
    editor: Editor,
    private val eventClassDialogDisplayName: String,
    private val eventClassName: String,
    defaultHandlerName: String
) : DialogWrapper(editor.project, editor.component, false, IdeModalityType.MODELESS) {

    private val graph = PropertyGraph("RedisHandlerNameSelectionDialog")
    private val handlerNameProperty = graph.property(defaultHandlerName)

    val chosenName by handlerNameProperty

    init {
        title = "Generate Redis Event Handler"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("$eventClassDialogDisplayName:") {
            label(eventClassName)
                .align(AlignX.FILL)
        }
        row("Handler name:") {
            textField()
                .bindText(handlerNameProperty)
                .columns(COLUMNS_LARGE)
                .focused()
        }
    }

    override fun getPreferredFocusedComponent() = null
}