package dev.slne.surf.idea.surfideaplugin.surfapi.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.NlsSafe
import dev.slne.surf.idea.surfideaplugin.surfapi.platform.SurfApiPlatform

class SurfApiFacet(
    facetType: FacetType<SurfApiFacet, SurfApiFacetConfiguration>,
    module: Module,
    name: @NlsSafe String,
    configuration: SurfApiFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<SurfApiFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {
    val detectedPlatforms: Set<SurfApiPlatform>
        get() = configuration.detectedPlatforms

    fun hasPaper(): Boolean = SurfApiPlatform.PAPER in detectedPlatforms
    fun hasVelocity(): Boolean = SurfApiPlatform.VELOCITY in detectedPlatforms
}