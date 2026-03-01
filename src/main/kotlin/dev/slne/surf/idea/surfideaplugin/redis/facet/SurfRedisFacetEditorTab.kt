package dev.slne.surf.idea.surfideaplugin.redis.facet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Text
import javax.swing.JComponent

class SurfRedisFacetEditorTab : FacetEditorTab() {
    override fun createComponent(): JComponent {
        return JewelComposePanel {
            Column {
                Row {
                    Text("Redis Facet Settings")
                }
            }
        }
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return SurfRedisFacetType.DISPLAY_NAME
    }

    override fun isModified(): Boolean {
        return false
    }
}