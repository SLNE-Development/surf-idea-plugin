package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.common

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinModCommandQuickFix
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

private val MUST_BE_INVOKED_ANNOTATIONS: Set<ClassId> = setOf(
    ClassId.topLevel(FqName("org.jetbrains.annotations.MustBeInvokedByOverriders")),
    ClassId.topLevel(FqName("javax.annotation.OverridingMethodsMustInvokeSuper")),
)

class MustBeInvokedByOverridersInspection :
    KotlinApplicableInspectionBase.Simple<KtNamedFunction, MustBeInvokedByOverridersInspection.Context>() {

    data class Context(
        val annotationShortName: String,
        val superMethodName: String,
        val superParameters: List<String>,
    )

    override fun getProblemDescription(
        element: KtNamedFunction,
        context: Context,
    ): @InspectionMessage String {
        return "Overriding method '${element.name}' does not call 'super.${context.superMethodName}()' " +
                "as required by @${context.annotationShortName}"
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

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val symbol = element.symbol
        val annotatedSuperSymbol = symbol.allOverriddenSymbols
            .filterNot { it.modality == KaSymbolModality.ABSTRACT }
            .firstOrNull { superSymbol ->
                superSymbol.annotations.any { annotation ->
                    annotation.classId in MUST_BE_INVOKED_ANNOTATIONS
                }
            }
            ?: return null

        if (containsSuperCall(element)) return null
        if (alwaysThrowsException(element)) return null

        val superMethodName = annotatedSuperSymbol.callableId?.callableName?.asString()
            ?: element.name
            ?: return null

        val paramNames = element.valueParameters.mapNotNull { it.name }

        return Context(
            annotationShortName = annotatedSuperSymbol.annotations.first { it.classId in MUST_BE_INVOKED_ANNOTATIONS }.classId?.shortClassName?.asString()
                ?: "UnknownAnnotation",
            superMethodName = superMethodName,
            superParameters = paramNames,
        )
    }

    override fun createQuickFix(
        element: KtNamedFunction,
        context: Context,
    ): KotlinModCommandQuickFix<KtNamedFunction> {
        return AddSuperCallFix(context)
    }

    private fun containsSuperCall(function: KtNamedFunction): Boolean {
        val body = function.bodyExpression ?: return false
        val functionName = function.name ?: return false
        var found = false

        body.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                if (found) return
                val receiver = expression.receiverExpression
                if (receiver is KtSuperExpression) {
                    val selector = expression.selectorExpression
                    if (selector is KtCallExpression) {
                        val calleeName = selector.getCallNameExpression()?.text
                        if (calleeName == functionName) {
                            found = true
                            return
                        }
                    }
                }
                super.visitDotQualifiedExpression(expression)
            }
        })

        return found
    }

    private fun alwaysThrowsException(function: KtNamedFunction): Boolean {
        val body = function.bodyExpression ?: return false
        if (body is KtThrowExpression) return true
        if (body is KtBlockExpression) {
            val statements = body.statements
            if (statements.size == 1 && statements.first() is KtThrowExpression) return true
        }
        return false
    }


    private class AddSuperCallFix(private val context: Context) : KotlinModCommandQuickFix<KtNamedFunction>() {
        override fun getFamilyName(): String = "Add 'super.${context.superMethodName}()' call"

        override fun getName(): String =
            "Add 'super.${context.superMethodName}(${context.superParameters.joinToString()})' call"

        override fun applyFix(
            project: Project,
            element: KtNamedFunction,
            updater: ModPsiUpdater,
        ) {
            val factory = KtPsiFactory(project)
            val paramsText = context.superParameters.joinToString(", ")
            val superCallText = "super.${context.superMethodName}($paramsText)"

            val body = element.bodyExpression ?: return

            when (body) {
                is KtBlockExpression -> {
                    val superCallStatement = factory.createExpression(superCallText)
                    val newLine = factory.createNewLine()
                    val lBrace = body.lBrace ?: return
                    body.addAfter(newLine, lBrace)
                    body.addAfter(superCallStatement, lBrace.nextSibling)
                }

                else -> {
                    val returnType = element.typeReference?.text
                    val existingExpressionText = body.text
                    val newBodyText = if (returnType != null && returnType != "Unit") {
                        "{\n$superCallText\nreturn $existingExpressionText\n}"
                    } else {
                        "{\n$superCallText\n$existingExpressionText\n}"
                    }
                    val newBody = factory.createBlock(newBodyText)

                    element.equalsToken?.delete()
                    element.bodyExpression?.replace(newBody)
                }
            }
        }
    }
}
