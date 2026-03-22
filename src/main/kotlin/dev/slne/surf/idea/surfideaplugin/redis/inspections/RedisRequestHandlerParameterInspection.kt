package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisRequestHandlerParameterInspection :
    RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, RedisRequestHandlerParameterInspection.Context>() {
    sealed interface Context {
        data object WrongParameterCount : Context
        data object WrongParameterType : Context
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val hasAnnotation = element.hasAnnotation(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID)
        if (!hasAnnotation) return null

        val valueParameters = element.valueParameters
        if (valueParameters.size != 1) return Context.WrongParameterCount

        val paramType = valueParameters.first().symbol.returnType as? KaClassType ?: return Context.WrongParameterType
        if (paramType.classId != SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID) return Context.WrongParameterType

        return null
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount ->
                "@HandleRedisRequest handler must have exactly 1 parameter (RequestContext<T : RedisRequest>), " +
                        "found ${element.valueParameters.size}"

            is Context.WrongParameterType ->
                "Parameter of @HandleRedisRequest handler must be RequestContext<T : RedisRequest>"
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