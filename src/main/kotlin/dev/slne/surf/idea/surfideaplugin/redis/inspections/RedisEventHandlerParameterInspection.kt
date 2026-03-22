package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisEventHandlerParameterInspection :
    RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, RedisEventHandlerParameterInspection.Context>() {
    sealed interface Context {
        data object WrongParameterCount : Context
        data object WrongParameterType : Context
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return KotlinPsiHeuristics.hasAnnotation(element, SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val valueParameters = element.valueParameters

        if (valueParameters.size != 1) {
            return Context.WrongParameterCount
        }

        val eventParam = valueParameters.first()
        val isSubtype = eventParam.symbol.returnType.isSubtypeOf(SurfRedisClassNames.REDIS_EVENT_CLASS_ID)

        return if (!isSubtype) Context.WrongParameterType else null
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount ->
                "@OnRedisEvent handler must have exactly 1 parameter (a RedisEvent subtype), " +
                        "found ${element.valueParameters.size}"

            is Context.WrongParameterType ->
                "Parameter of @OnRedisEvent handler must be a subtype of RedisEvent"
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