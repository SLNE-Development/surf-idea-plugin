package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.common.util.shortNameString
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

/**
 * surf-redis dispatches events by the exact runtime class of the published event — there is
 * no polymorphic lookup. A handler whose parameter is an abstract (or sealed) event type can
 * therefore never receive an event.
 */
class RedisEventHandlerAbstractEventInspection :
    SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_REDIS_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (element.valueParameters.size != 1) return false
        return element.hasAnnotationPsi(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val eventParameter = element.valueParameters.singleOrNull() ?: return null

        val eventType = eventParameter.symbol.returnType as? KaClassType ?: return null
        if (!eventType.isSubtypeOf(SurfRedisClassNames.REDIS_EVENT_CLASS_ID)) return null

        val eventClass = eventType.symbol
        val isAbstract =
            eventClass.modality == KaSymbolModality.ABSTRACT || eventClass.modality == KaSymbolModality.SEALED

        return isAbstract.asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) {
            it.valueParameters.firstOrNull()?.typeReference ?: it.valueParameterList ?: it
        }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            buildString {
                append("@")
                append(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN.shortNameString())
                append(" dispatches by exact event type — a handler for an abstract event type will never be invoked")
            },
            ProblemHighlightType.WARNING,
            onTheFly,
        )
    }
}
