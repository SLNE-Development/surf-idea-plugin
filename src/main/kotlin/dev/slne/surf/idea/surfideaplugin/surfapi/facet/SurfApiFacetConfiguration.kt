package dev.slne.surf.idea.surfideaplugin.surfapi.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.XmlSerializerUtil
import dev.slne.surf.idea.surfideaplugin.surfapi.platform.SurfApiPlatform
import java.util.*

class SurfApiFacetConfiguration : FacetConfiguration, PersistentStateComponent<SurfApiFacetConfiguration> {
    var detectedPlatforms: EnumSet<SurfApiPlatform> = EnumSet.of(SurfApiPlatform.CORE)

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<out FacetEditorTab> {
        return emptyArray()
    }

    override fun getState(): SurfApiFacetConfiguration {
        return this
    }

    override fun loadState(state: SurfApiFacetConfiguration) {
        XmlSerializerUtil.copyBean(state, this)
    }
}