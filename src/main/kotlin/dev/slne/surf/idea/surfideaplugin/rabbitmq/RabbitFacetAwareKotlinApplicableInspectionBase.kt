package dev.slne.surf.idea.surfideaplugin.rabbitmq

import com.intellij.facet.FacetTypeId
import dev.slne.surf.idea.surfideaplugin.common.facet.FacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.rabbitmq.facet.SurfRabbitFacetType
import org.jetbrains.kotlin.psi.KtElement

abstract class RabbitFacetAwareKotlinApplicableInspectionBase<E : KtElement, C : Any> :
    FacetAwareKotlinApplicableInspectionBase<E, C>() {
    override fun requiredFacetType(): FacetTypeId<*> = SurfRabbitFacetType.ID
}