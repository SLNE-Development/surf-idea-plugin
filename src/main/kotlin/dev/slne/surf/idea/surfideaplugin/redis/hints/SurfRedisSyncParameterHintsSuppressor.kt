package dev.slne.surf.idea.surfideaplugin.redis.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.ParameterNameHintsSuppressor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

@Suppress("UnstableApiUsage")
class SurfRedisSyncParameterHintsSuppressor : ParameterNameHintsSuppressor {
    override fun isSuppressedFor(
        file: PsiFile,
        inlayInfo: InlayInfo
    ): Boolean {
        val element = file.findElementAt(inlayInfo.offset) ?: return false

        val valueArgument = element.parentOfType<KtValueArgument>() ?: return false
        val argumentList = valueArgument.parent as? KtValueArgumentList ?: return false

        val callExpression = argumentList.parent as? KtCallExpression ?: return false
        val calleeName = callExpression.calleeExpression?.text ?: return false

        if (calleeName !in SurfRedisSyncKeyInlayHintsProvider.Util.SYNC_METHODS) return false

        val argIndex = argumentList.arguments.indexOf(valueArgument)

        val argumentName = valueArgument.getArgumentName()?.asName?.asString()
        return argumentName == "id" || (argumentName == null && argIndex == 0)
    }
}