package dev.slne.surf.idea.surfideaplugin.common.postfix.filters

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis

/**
 * Filters Kotlin expressions that represent statements.
 *
 * Returns `true` only if the expression, resolved to its outermost qualified
 * form via [getQualifiedExpressionForReceiverOrThis], is considered a statement
 * by [KtPsiUtil.isStatement].
 *
 * Intended for use in postfix templates that should only apply to
 * standalone statement-level expressions, not to sub-expressions or receivers.
 *
 * @see <a href="https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/code-insight/postfix-templates/src/org/jetbrains/kotlin/idea/codeInsight/postfix/KotlinPostfixTemplate.kt#L76">KotlinPostfixTemplate</a>
 */
object StatementFilter : (KtExpression) -> Boolean {
    override fun invoke(expression: KtExpression): Boolean {
        return KtPsiUtil.isStatement(expression.getQualifiedExpressionForReceiverOrThis())
    }
}