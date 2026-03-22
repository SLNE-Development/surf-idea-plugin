package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import dev.slne.surf.idea.surfideaplugin.common.util.findValueParameter
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareAbstractKotlinInspection
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.RemoveArgumentFix
import org.jetbrains.kotlin.idea.quickfix.RemovePsiElementSimpleFix
import org.jetbrains.kotlin.idea.quickfix.RemoveRedundantReturnFix
import org.jetbrains.kotlin.psi.*

class RedisRequestHandlerContextLeakInspection : RedisFacetAwareAbstractKotlinInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = namedFunctionVisitor(fun(element) {
        if (!KotlinPsiHeuristics.hasAnnotation(element, SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN)) return
        if (!element.hasBlockBody()) return

        val contextName = analyze(element) {
            if (!element.hasAnnotation(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID)) return@analyze null
            element.findValueParameter(SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID)?.name
        } ?: return

        checkForLeaks(element, contextName, holder)
    })

    private fun checkForLeaks(function: KtNamedFunction, contextName: String, holder: ProblemsHolder) {
        function.bodyExpression?.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                if (isContextRespondCall(expression, contextName)) return

                for (arg in expression.valueArguments) {
                    val argExpr = arg.getArgumentExpression()
                    if (argExpr is KtNameReferenceExpression && argExpr.getReferencedName() == contextName) {
                        holder.registerProblem(
                            argExpr,
                            "RequestContext passed outside the @HandleRedisRequest handler",
                            RemoveArgumentFix(arg).asQuickFix()
                        )
                    }
                }
            }

            override fun visitReturnExpression(expression: KtReturnExpression) {
                super.visitReturnExpression(expression)

                val returnedExpr = expression.returnedExpression
                if (returnedExpr is KtNameReferenceExpression && returnedExpr.getReferencedName() == contextName) {
                    holder.registerProblem(
                        expression,
                        "RequestContext returned from the @HandleRedisRequest handler",
                        RemoveRedundantReturnFix(expression).asQuickFix()
                    )
                }
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)

                if (expression.operationToken != org.jetbrains.kotlin.lexer.KtTokens.EQ) return

                val right = expression.right
                if (right is KtNameReferenceExpression && right.getReferencedName() == contextName) {
                    val left = expression.left

                    if (left is KtDotQualifiedExpression || left is KtArrayAccessExpression) {
                        holder.registerProblem(
                            expression,
                            "RequestContext assigned outside the @HandleRedisRequest handler",
                        )
                    }
                }
            }

            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)

                val initializer = property.initializer
                if (initializer is KtNameReferenceExpression && initializer.getReferencedName() == contextName) {
                    holder.registerProblem(
                        property,
                        "RequestContext assigned to a property",
                        *IntentionWrapper.wrapToQuickFixes(
                            RemovePsiElementSimpleFix.RemoveVariableFactory.createQuickFix(property).toTypedArray(),
                            property.containingFile
                        )
                    )
                }
            }
        })
    }

    private fun isContextRespondCall(call: KtCallExpression, contextName: String): Boolean {
        val callee = call.calleeExpression ?: return false
        if (callee.text != SurfRedisConstants.RESPOND_METHOD_NAME) return false

        val dotQualified = call.parent as? KtDotQualifiedExpression ?: return false
        val receiver = dotQualified.receiverExpression as? KtNameReferenceExpression ?: return false

        return receiver.getReferencedName() == contextName
    }
}