package dev.slne.surf.idea.surfideaplugin.redis.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.parents
import com.intellij.util.Function
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis

internal fun redisExpressionSelector(superTypeFqn: String): PostfixTemplateExpressionSelector {
    val superClassId = ClassId.topLevel(FqName(superTypeFqn))

    return object : PostfixTemplateExpressionSelector {
        override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean {
            return getExpressions(context, copyDocument, newOffset).isNotEmpty()
        }

        override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
            return collectExpressions(context.containingFile, offset)
                .filter { isSubtypeOf(it, superClassId) }
                .toList()
        }

        override fun getRenderer(): Function<PsiElement, String> = Function(PsiElement::getText)
    }
}

private fun collectExpressions(file: PsiFile, offset: Int): Sequence<KtExpression> {
    val elementAtOffset = PsiUtilCore.getElementAtOffset(file, offset - 1)
    return elementAtOffset.parents(true)
        .filterIsInstance<KtExpression>()
        .filter { it.endOffset == offset }
        .filter { expression ->
            val parent = expression.parent
            when {
                expression is KtBlockExpression -> false
                expression is KtFunctionLiteral -> false
                expression is KtLambdaExpression && parent is KtLambdaArgument -> false
                parent is KtThisExpression -> false
                parent is KtQualifiedExpression && expression == parent.selectorExpression -> false
                expression.node.elementType == KtNodeTypes.OPERATION_REFERENCE -> false
                parent is KtPackageDirective || parent is KtImportDirective -> false
                else -> true
            }
        }
        .filter { expression ->
            when {
                KtPsiUtil.isAssignment(expression) -> false
                expression is KtNamedDeclaration && expression.name != null -> false
                expression is KtLoopExpression -> false
                expression is KtReturnExpression -> false
                expression is KtBreakExpression -> false
                expression is KtContinueExpression -> false
                else -> true
            }
        }
        .filter { KtPsiUtil.isStatement(it.getQualifiedExpressionForReceiverOrThis()) }
}

@OptIn(KaAllowAnalysisOnEdt::class)
private fun isSubtypeOf(expression: KtExpression, superClassId: ClassId): Boolean {
    return allowAnalysisOnEdt {
        @OptIn(KaAllowAnalysisFromWriteAction::class)
        allowAnalysisFromWriteAction {
            analyze(expression) {
                val exprType = expression.expressionType ?: return@analyze false
                val superSymbol = findClass(superClassId) ?: return@analyze false
                val superType = superSymbol.defaultType
                exprType.isSubtypeOf(superType)
            }
        }
    }
}