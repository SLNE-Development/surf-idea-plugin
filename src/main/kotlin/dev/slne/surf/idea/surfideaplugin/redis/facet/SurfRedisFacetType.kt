package dev.slne.surf.idea.surfideaplugin.redis.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.NlsSafe
import javax.swing.Icon

class SurfRedisFacetType : FacetType<SurfRedisFacet, SurfRedisFacetConfiguration>(ID, STRING_ID, DISPLAY_NAME) {
    companion object {
        val ID = FacetTypeId<SurfRedisFacet>("surf-redis")
        const val STRING_ID = "surf-redis"
        const val DISPLAY_NAME = "Surf Redis"
    }

    override fun createDefaultConfiguration() = SurfRedisFacetConfiguration()

    override fun createFacet(
        module: Module,
        name: @NlsSafe String,
        configuration: SurfRedisFacetConfiguration,
        underlyingFacet: Facet<*>?
    ): SurfRedisFacet = SurfRedisFacet(this, module, name, configuration, underlyingFacet)

    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = true

    override fun getIcon(): Icon? {
        return super.getIcon()
    }
}