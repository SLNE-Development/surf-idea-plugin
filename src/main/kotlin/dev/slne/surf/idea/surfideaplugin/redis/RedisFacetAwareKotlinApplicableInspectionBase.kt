package dev.slne.surf.idea.surfideaplugin.redis

import com.intellij.facet.FacetTypeId
import dev.slne.surf.idea.surfideaplugin.common.facet.FacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.facet.SurfRedisFacetType
import org.jetbrains.kotlin.psi.KtElement

abstract class RedisFacetAwareKotlinApplicableInspectionBase<E : KtElement, C : Any> :
    FacetAwareKotlinApplicableInspectionBase<E, C>() {
    override fun requiredFacetType(): FacetTypeId<*> = SurfRedisFacetType.ID
}