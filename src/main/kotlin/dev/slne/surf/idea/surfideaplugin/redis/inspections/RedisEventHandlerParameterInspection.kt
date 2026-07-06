package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.common.util.isReturnType
import dev.slne.surf.idea.surfideaplugin.common.util.shortNameString
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.inspections.RedisEventHandlerParameterInspection.Context
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

/**
 * Verifies the parameters of @[SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION] annotated functions.
 */
class RedisEventHandlerParameterInspection :
    SurfApplicableInspection<KtNamedFunction, Context>(SurfLibraryMarker.SURF_REDIS_API) {
    sealed interface Context {
        data class WrongParameterCount(val actualCount: Int) : Context
        data object WrongParameterType : Context
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isSurfApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.hasAnnotationPsi(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val valueParameters = element.valueParameters

        if (valueParameters.size != 1) {
            return Context.WrongParameterCount(valueParameters.size)
        }

        val eventParameter = valueParameters.single()
        val isRedisEventType = eventParameter.isReturnType(SurfRedisClassNames.REDIS_EVENT_CLASS_ID)

        return if (!isRedisEventType) Context.WrongParameterType else null
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList ?: it.nameIdentifier ?: it }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount -> buildString {
                append("@")
                append(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN.shortNameString())
                append(" handler must have exactly 1 parameter of type ")
                append(SurfRedisClassNames.REDIS_EVENT_CLASS_FQN.shortNameString())
                append(", but found ")
                if (context.actualCount == 0) append("none") else append(context.actualCount)
                append(" parameter(s)")
            }

            Context.WrongParameterType -> buildString {
                append("Parameter of @")
                append(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN.shortNameString())
                append(" handler must be a subtype of ")
                append(SurfRedisClassNames.REDIS_EVENT_CLASS_FQN.shortNameString())
            }
        }

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}