package dev.slne.surf.idea.surfideaplugin.common.postfix.filters

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.idea.base.analysis.api.utils.allowAnalysisFromWriteActionInEdt
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Filters Kotlin expressions based on their resolved [KaType].
 *
 * Resolves the type of the expression using the Kotlin Analysis API and
 * evaluates [predicate] against it. Returns `false` if the type cannot
 * be resolved.
 *
 * The type resolution runs inside [allowAnalysisFromWriteActionInEdt] to
 * allow safe Analysis API access even when called from a write action on
 * the EDT.
 *
 * @param predicate A context function receiving a [KaSession] that determines
 * whether the resolved [KaType] qualifies for the postfix template.
 *
 * @see <a href="https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/code-insight/postfix-templates/src/org/jetbrains/kotlin/idea/codeInsight/postfix/KotlinPostfixTemplate.kt#L82">KotlinPostfixTemplate</a>
 */
class ExpressionTypeFilter(val predicate: context(KaSession) (KaType) -> Boolean) : (KtExpression) -> Boolean {
    override fun invoke(expression: KtExpression): Boolean {
        allowAnalysisFromWriteActionInEdt(expression) {
            val type = it.expressionType
            return type != null && predicate(type)
        }
    }
}