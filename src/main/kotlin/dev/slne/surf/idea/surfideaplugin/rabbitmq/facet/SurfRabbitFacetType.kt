package dev.slne.surf.idea.surfideaplugin.rabbitmq.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.NlsSafe
import javax.swing.Icon


class SurfRabbitFacetType : FacetType<SurfRabbitFacet, SurfRabbitFacetConfiguration>(ID, STRING_ID, DISPLAY_NAME) {
    companion object {
        val ID = FacetTypeId<SurfRabbitFacet>("surf-rabbit")
        const val STRING_ID = "surf-rabbit"
        const val DISPLAY_NAME = "Surf RabbitMQ"
    }

    override fun createDefaultConfiguration() = SurfRabbitFacetConfiguration()

    override fun createFacet(
        module: Module,
        name: @NlsSafe String,
        configuration: SurfRabbitFacetConfiguration,
        underlyingFacet: Facet<*>?
    ): SurfRabbitFacet = SurfRabbitFacet(this, module, name, configuration, underlyingFacet)

    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = true
    override fun getIcon(): Icon? {
        return super.getIcon()
    }
}