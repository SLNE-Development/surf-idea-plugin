package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.*

class RedisRequestHandlerMissingRespondInspection :
    SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_REDIS_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!element.hasBlockBody()) return false
        if (element.valueParameters.size != 1) return false

        return element.hasAnnotationPsi(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val contextParameter = element.valueParameters.singleOrNull() ?: return null

        val paramType = contextParameter.symbol.returnType as? KaClassType ?: return null
        if (paramType.classId != SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID) return null

        val contextName = contextParameter.name ?: return null
        val bodyExpression = element.bodyExpression ?: return null

        if (bodyExpression.hasRespondCallOn(contextName)) {
            return null
        }

        return Unit
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
        val functionName = element.name ?: "<anonymous>"

        return createProblemDescriptor(
            element,
            rangeInElement,
            "@HandleRedisRequest handler '$functionName' must call '${SurfRedisConstants.RESPOND_METHOD_NAME}(...)' on the RequestContext.",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }

    private fun KtExpression.hasRespondCallOn(
        contextName: String,
    ): Boolean {
        var found = false

        accept(object : KtTreeVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                if (found) return
                super.visitElement(element)
            }

            override fun visitDotQualifiedExpression(
                expression: KtDotQualifiedExpression,
            ) {
                if (found) return

                if (expression.isRespondCallOn(contextName)) {
                    found = true
                    return
                }

                super.visitDotQualifiedExpression(expression)
            }
        })

        return found
    }

    private fun KtDotQualifiedExpression.isRespondCallOn(
        contextName: String,
    ): Boolean {
        val receiver = receiverExpression as? KtNameReferenceExpression ?: return false
        if (receiver.getReferencedName() != contextName) return false

        val call = selectorExpression as? KtCallExpression ?: return false
        val calleeName = call.calleeExpression?.text ?: return false

        return calleeName == SurfRedisConstants.RESPOND_METHOD_NAME
    }
}