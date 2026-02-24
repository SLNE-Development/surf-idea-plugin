package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

class RedisRequestHandlerMissingRespondInspection : KotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    private val handleRedisRequestAnnotation = FqName(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION)
    private val handleRedisRequestAnnotationClassId = ClassId.topLevel(handleRedisRequestAnnotation)
    private val requestContextClassId = ClassId.topLevel(FqName(SurfRedisClassNames.REQUEST_CONTEXT_CLASS))

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        if (!KotlinPsiHeuristics.hasAnnotation(element, handleRedisRequestAnnotation)) return false
        return element.hasBlockBody() && element.valueParameters.size == 1
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val hasAnnotation = element.symbol.annotations.any { annotation ->
            annotation.classId == handleRedisRequestAnnotationClassId
        }
        if (!hasAnnotation) return null

        val contextParam = element.valueParameters.firstOrNull() ?: return null
        val paramType = contextParam.symbol.returnType as? KaClassType ?: return null
        if (paramType.classId != requestContextClassId) return null

        val contextName = contextParam.name ?: return null
        if (hasRespondCall(element, contextName)) return null

        return Unit
    }

    private fun hasRespondCall(function: KtNamedFunction, contextName: String): Boolean {
        var found = false

        function.bodyExpression?.accept(callExpressionRecursiveVisitor { element ->
            if (found) return@callExpressionRecursiveVisitor

            val callee = element.calleeExpression ?: return@callExpressionRecursiveVisitor
            if (callee.text != SurfRedisConstants.RESPOND_METHOD_NAME) return@callExpressionRecursiveVisitor

            val dotQualified = element.parent as? KtDotQualifiedExpression ?: return@callExpressionRecursiveVisitor
            val receiver = dotQualified.receiverExpression
            if (receiver is KtNameReferenceExpression && receiver.getReferencedName() == contextName) {
                found = true
            }
        })

        return found
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