package dev.slne.surf.idea.surfideaplugin.redis

import com.intellij.facet.FacetTypeId
import dev.slne.surf.idea.surfideaplugin.common.facet.FacetAwareAbstractKotlinInspection
import dev.slne.surf.idea.surfideaplugin.redis.facet.SurfRedisFacetType

abstract class RedisFacetAwareAbstractKotlinInspection: FacetAwareAbstractKotlinInspection() {
    override fun requiredFacetType(): FacetTypeId<*> = SurfRedisFacetType.ID
}