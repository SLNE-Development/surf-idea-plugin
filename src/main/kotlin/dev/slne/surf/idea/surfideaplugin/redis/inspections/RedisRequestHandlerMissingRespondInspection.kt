package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.*

class RedisRequestHandlerMissingRespondInspection :
    RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.hasBlockBody() && element.valueParameters.size == 1
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val hasAnnotation = element.hasAnnotation(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID)
        if (!hasAnnotation) return null

        val contextParam = element.valueParameters.firstOrNull() ?: return null
        val paramType = contextParam.symbol.returnType as? KaClassType ?: return null
        if (paramType.classId != SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID) return null

        val contextName = contextParam.name ?: return null
        if (hasRespondCall(element, contextName)) return null

        return Unit
    }

    /**
     * Checks if the given function contains a call to a specific method (respond method) on the provided context name.
     *
     * @param function The Kotlin `KtNamedFunction` to analyze for the method call.
     * @param contextName The name of the context object to check for method calls on.
     * @return `true` if the function contains a call to the respond method on the specified context name; `false` otherwise.
     */
    private fun hasRespondCall(function: KtNamedFunction, contextName: String): Boolean {
        var found = false

        function.bodyExpression?.accept(callExpressionRecursiveVisitor(fun(element) {
            if (found) return

            val callee = element.calleeExpression ?: return
            if (callee.text != SurfRedisConstants.RESPOND_METHOD_NAME) return

            val dotQualified = element.parent as? KtDotQualifiedExpression ?: return
            val receiver = dotQualified.receiverExpression
            if (receiver is KtNameReferenceExpression && receiver.getReferencedName() == contextName) {
                found = true
            }
        }))

        return found
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
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
            "@HandleRedisRequest handler '${element.name}' must call '${SurfRedisConstants.RESPOND_METHOD_NAME}(...)' on the RequestContext",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}