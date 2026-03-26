package dev.slne.surf.idea.surfideaplugin.rabbitmq.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.NlsSafe

class SurfRabbitFacet(
    facetType: FacetType<SurfRabbitFacet, SurfRabbitFacetConfiguration>,
    module: Module,
    name: @NlsSafe String,
    configuration: SurfRabbitFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<SurfRabbitFacetConfiguration>(facetType, module, name, configuration, underlyingFacet)