package dev.slne.surf.idea.surfideaplugin.common.postfix.filters

import org.jetbrains.kotlin.psi.*

/**
 * Filters Kotlin expressions that can be treated as value-producing expressions.
 *
 * Returns `true` for expressions that yield a usable value and `false` for
 * declarations, assignments, loops, and control-flow expressions that do not
 * fit postfix template value semantics.
 *
 * Excluded expressions:
 * - Assignments
 * - Named declarations, except anonymous functions
 * - Loops
 * - `return`, `break`, and `continue`
 * - `if` expressions without an `else` branch
 *
 * Anonymous functions are allowed because they can be used as expressions.[web:5][web:6]
 *
 * @see <a href="https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/code-insight/postfix-templates/src/org/jetbrains/kotlin/idea/codeInsight/postfix/KotlinPostfixTemplate.kt#L58">KotlinPostfixTemplate</a>
 */
object ValuedFilter : (KtExpression) -> Boolean {

    override fun invoke(expression: KtExpression): Boolean {
        val isAnonymousFunction =
            expression is KtFunctionLiteral || (expression is KtNamedFunction && expression.name == null)

        return when {
            KtPsiUtil.isAssignment(expression) -> false
            expression is KtNamedDeclaration && !isAnonymousFunction -> false
            expression is KtLoopExpression -> false
            expression is KtReturnExpression -> false
            expression is KtBreakExpression -> false
            expression is KtContinueExpression -> false
            expression is KtIfExpression && expression.`else` == null -> false
            else -> true
        }
    }
}