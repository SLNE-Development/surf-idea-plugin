package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfKotlinInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.quickfix.RemoveReturnTypeQuickFix
import dev.slne.surf.idea.surfideaplugin.common.util.findValueParameter
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.RemoveArgumentFix
import org.jetbrains.kotlin.idea.quickfix.RemovePsiElementSimpleFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class RedisRequestHandlerContextLeakInspection : SurfKotlinInspection(SurfLibraryMarker.SURF_REDIS_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = namedFunctionVisitor(fun(function) {
        if (!function.hasAnnotationPsi(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN)) return
        val bodyExpression = function.bodyExpression ?: return

        val contextName = analyze(function) {
            function.findValueParameter(SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID)?.name
        } ?: return

        bodyExpression.accept(
            RequestContextLeakVisitor(
                contextName,
                holder
            )
        )
    })

    private class RequestContextLeakVisitor(
        private val contextName: String,
        private val holder: ProblemsHolder,
    ) : KtTreeVisitorVoid() {

        override fun visitCallExpression(expression: KtCallExpression) {
            checkCallArguments(expression)
            super.visitCallExpression(expression)
        }

        override fun visitReturnExpression(expression: KtReturnExpression) {
            checkReturnExpression(expression)
            super.visitReturnExpression(expression)
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            checkAssignment(expression)
            super.visitBinaryExpression(expression)
        }

        override fun visitProperty(property: KtProperty) {
            checkPropertyInitializer(property)
            super.visitProperty(property)
        }

        private fun checkCallArguments(expression: KtCallExpression) {
            for (argument in expression.valueArguments) {
                val argumentExpression = argument.getArgumentExpression()

                if (!argumentExpression.isContextReference()) {
                    continue
                }

                holder.registerProblem(
                    argumentExpression,
                    "RequestContext passed outside the @HandleRedisRequest handler.",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    RemoveArgumentFix(argument).asQuickFix(),
                )
            }
        }

        private fun checkReturnExpression(expression: KtReturnExpression) {
            val returnedExpression = expression.returnedExpression

            if (!returnedExpression.isContextReference()) {
                return
            }

            holder.registerProblem(
                expression,
                "RequestContext returned from the @HandleRedisRequest handler.",
                ProblemHighlightType.GENERIC_ERROR,
                RemoveReturnTypeQuickFix(),
            )
        }

        private fun checkAssignment(expression: KtBinaryExpression) {
            if (expression.operationToken != KtTokens.EQ) {
                return
            }

            val right = expression.right
            if (!right.isContextReference()) {
                return
            }

            val left = expression.left
            val assignsOutsideLocalVariable = left is KtDotQualifiedExpression || left is KtArrayAccessExpression

            if (!assignsOutsideLocalVariable) {
                return
            }

            holder.registerProblem(
                expression,
                "RequestContext assigned outside the @HandleRedisRequest handler.",
                ProblemHighlightType.GENERIC_ERROR,
            )
        }

        private fun checkPropertyInitializer(property: KtProperty) {
            val initializer = property.initializer

            if (!initializer.isContextReference()) {
                return
            }

            holder.registerProblem(
                property,
                "RequestContext assigned to a variable or property.",
                ProblemHighlightType.GENERIC_ERROR,
                *IntentionWrapper.wrapToQuickFixes(
                    RemovePsiElementSimpleFix.RemoveVariableFactory
                        .createQuickFix(property)
                        .toTypedArray(),
                    property.containingFile,
                ),
            )
        }

        @OptIn(ExperimentalContracts::class)
        private fun KtExpression?.isContextReference(): Boolean {
            contract {
                returns(true) implies (this@isContextReference != null)
            }

            return this is KtNameReferenceExpression && getReferencedName() == contextName
        }
    }
}