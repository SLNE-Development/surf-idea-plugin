package dev.slne.surf.idea.surfideaplugin.rabbitmq

import com.intellij.facet.FacetTypeId
import dev.slne.surf.idea.surfideaplugin.common.facet.FacetAwareAbstractKotlinInspection
import dev.slne.surf.idea.surfideaplugin.rabbitmq.facet.SurfRabbitFacetType

abstract class RabbitFacetAwareAbstractKotlinInspection : FacetAwareAbstractKotlinInspection() {
    override fun requiredFacetType(): FacetTypeId<*> = SurfRabbitFacetType.ID
}