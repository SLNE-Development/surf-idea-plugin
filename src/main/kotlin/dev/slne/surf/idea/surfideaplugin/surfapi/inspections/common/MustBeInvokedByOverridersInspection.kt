package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.common

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnyAnnotation
import dev.slne.surf.idea.surfideaplugin.surfapi.inspections.common.MustBeInvokedByOverridersInspection.Context
import dev.slne.surf.idea.surfideaplugin.util.FqClassNameSet
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinModCommandQuickFix
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

private val MUST_BE_INVOKED_ANNOTATIONS = FqClassNameSet(
    "org.jetbrains.annotations.MustBeInvokedByOverriders",
    "javax.annotation.OverridingMethodsMustInvokeSuper",
)

class MustBeInvokedByOverridersInspection : KotlinApplicableInspectionBase.Simple<KtNamedFunction, Context>() {

    data class Context(
        val annotationShortName: String,
        val superMethodName: String,
        val superArguments: List<String>,
        val returnsUnit: Boolean,
    ) {
        val superCallText: String
            get() = buildString {
                append("super.")
                append(superMethodName)
                append("(")
                append(superArguments.joinToString(", "))
                append(")")
            }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        if (element.bodyExpression == null) return false
        return true
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val functionSymbol = element.symbol
        val annotatedSuperSymbol = functionSymbol.allOverriddenSymbols
            .filterNot { it.modality == KaSymbolModality.ABSTRACT }
            .firstOrNull { it.hasAnyAnnotation(MUST_BE_INVOKED_ANNOTATIONS) }
            ?: return null

        val superMethodName = annotatedSuperSymbol.callableId
            ?.callableName
            ?.asString()
            ?: element.name
            ?: return null

        if (containsSuperCall(element, superMethodName)) return null
        if (alwaysThrowsException(element)) return null

        val annotationClassId = annotatedSuperSymbol.annotations
            .firstNotNullOfOrNull { annotation ->
                annotation.classId?.takeIf { it in MUST_BE_INVOKED_ANNOTATIONS }
            }
            ?: return null

        return Context(
            annotationShortName = annotationClassId.shortClassName.asString(),
            superMethodName = superMethodName,
            superArguments = element.valueParameters.mapNotNull { it.name },
            returnsUnit = functionSymbol.returnType.isUnitType,
        )
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun getProblemDescription(
        element: KtNamedFunction,
        context: Context,
    ): @InspectionMessage String = buildString {
        append("Overriding method '")
        append(element.name ?: context.superMethodName)
        append("' does not call '")
        append(context.superCallText)
        append("' as required by @")
        append(context.annotationShortName)
    }

    override fun createQuickFix(
        element: KtNamedFunction,
        context: Context,
    ): KotlinModCommandQuickFix<KtNamedFunction> {
        return AddSuperCallFix(context)
    }

    private fun containsSuperCall(function: KtNamedFunction, superMethodName: String): Boolean {
        val body = function.bodyExpression ?: return false
        var found = false

        body.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                if (found) return

                val receiver = expression.receiverExpression
                if (receiver !is KtSuperExpression) {
                    super.visitDotQualifiedExpression(expression)
                    return
                }

                val selector = expression.selectorExpression as? KtCallExpression
                if (selector == null) {
                    super.visitDotQualifiedExpression(expression)
                    return
                }

                val calleeName = selector.getCallNameExpression()?.text
                if (calleeName == superMethodName) {
                    found = true
                    return
                }

                super.visitDotQualifiedExpression(expression)
            }
        })

        return found
    }

    private fun alwaysThrowsException(function: KtNamedFunction): Boolean = when (val body = function.bodyExpression) {
        is KtThrowExpression -> true

        is KtBlockExpression -> {
            val statements = body.statements
            statements.size == 1 && statements.single() is KtThrowExpression
        }

        else -> false
    }


    private class AddSuperCallFix(private val context: Context) : KotlinModCommandQuickFix<KtNamedFunction>() {
        override fun getFamilyName(): String {
            return "Add required super call"
        }

        override fun getName(): String {
            return "Add '${context.superCallText}' call"
        }

        override fun applyFix(
            project: Project,
            element: KtNamedFunction,
            updater: ModPsiUpdater,
        ) {
            val factory = KtPsiFactory(project)
            val body = element.bodyExpression ?: return

            when (body) {
                is KtBlockExpression -> {
                    addSuperCallToBlockBody(
                        factory = factory,
                        body = body,
                        superCallText = context.superCallText,
                    )
                }

                else -> {
                    replaceExpressionBodyWithBlockBody(
                        factory = factory,
                        function = element,
                        bodyText = body.text,
                        context = context,
                    )
                }
            }
        }

        private fun addSuperCallToBlockBody(
            factory: KtPsiFactory,
            body: KtBlockExpression,
            superCallText: String,
        ) {
            val lBrace = body.lBrace ?: return
            val superCall = factory.createExpression(superCallText)

            val insertedSuperCall = body.addAfter(superCall, lBrace)
            body.addAfter(factory.createNewLine(), insertedSuperCall)
            body.addAfter(factory.createNewLine(), lBrace)
        }

        private fun replaceExpressionBodyWithBlockBody(
            factory: KtPsiFactory,
            function: KtNamedFunction,
            bodyText: String,
            context: Context,
        ) {
            val newBodyText = buildString {
                appendLine("{")
                appendLine(context.superCallText)

                if (context.returnsUnit) {
                    appendLine(bodyText)
                } else {
                    append("return ")
                    appendLine(bodyText)
                }

                append("}")
            }

            val newBody = factory.createBlock(newBodyText)

            function.bodyExpression?.replace(newBody)
            function.equalsToken?.delete()
        }
    }
}
