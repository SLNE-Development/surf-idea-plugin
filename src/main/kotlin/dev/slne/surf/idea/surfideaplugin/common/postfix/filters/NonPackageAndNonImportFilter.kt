package dev.slne.surf.idea.surfideaplugin.common.postfix.filters

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

/**
 * Filters out expressions whose parent is a package or import directive.
 *
 * Returns `false` if the expression's direct parent is a [KtPackageDirective]
 * or [KtImportDirective], and `true` otherwise.
 *
 * Prevents postfix templates from being offered inside `package` or `import`
 * statements, where they are not applicable.
 *
 * @see <a href="https://github.com/JetBrains/intellij-community/blob/master/plugins/kotlin/code-insight/postfix-templates/src/org/jetbrains/kotlin/idea/codeInsight/postfix/KotlinPostfixTemplate.kt#L97">KotlinPostfixTemplate</a>
 */
object NonPackageAndNonImportFilter : (KtExpression) -> Boolean {
    override fun invoke(expression: KtExpression): Boolean {
        val parent = expression.parent
        return parent !is KtPackageDirective && parent !is KtImportDirective
    }
}