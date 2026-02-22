package dev.slne.surf.idea.surfideaplugin.surfapi.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.NlsSafe

class SurfApiFacetType : FacetType<SurfApiFacet, SurfApiFacetConfiguration>(ID, STRING_ID, DISPLAY_NAME) {
    companion object {
        val ID = FacetTypeId<SurfApiFacet>("surf-api")
        const val STRING_ID = "surf-api"
        const val DISPLAY_NAME = "Surf API"
    }

    override fun createDefaultConfiguration() = SurfApiFacetConfiguration()

    override fun createFacet(
        module: Module,
        name: @NlsSafe String,
        configuration: SurfApiFacetConfiguration,
        underlyingFacet: Facet<*>?
    ) = SurfApiFacet(this, module, name, configuration, underlyingFacet)

    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = true
}