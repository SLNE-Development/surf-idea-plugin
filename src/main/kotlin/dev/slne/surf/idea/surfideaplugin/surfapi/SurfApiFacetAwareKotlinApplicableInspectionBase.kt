package dev.slne.surf.idea.surfideaplugin.surfapi

import com.intellij.facet.FacetTypeId
import dev.slne.surf.idea.surfideaplugin.common.facet.FacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.surfapi.facet.SurfApiFacetType
import org.jetbrains.kotlin.psi.KtElement

abstract class SurfApiFacetAwareKotlinApplicableInspectionBase<E : KtElement, C : Any> :
    FacetAwareKotlinApplicableInspectionBase<E, C>() {
    override fun requiredFacetType(): FacetTypeId<*> = SurfApiFacetType.ID
}