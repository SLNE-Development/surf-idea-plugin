package dev.slne.surf.idea.surfideaplugin.rabbitmq.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager

class SurfRabbitFacetConfiguration : FacetConfiguration {
    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<out FacetEditorTab> {
        return arrayOf(SurfRabbitFacetEditorTab())
    }
}