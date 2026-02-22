package dev.slne.surf.idea.surfideaplugin.redis.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.NlsSafe

class SurfRedisFacet(
    facetType: FacetType<SurfRedisFacet, SurfRedisFacetConfiguration>,
    module: Module,
    name: @NlsSafe String,
    configuration: SurfRedisFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<SurfRedisFacetConfiguration>(facetType, module, name, configuration, underlyingFacet)