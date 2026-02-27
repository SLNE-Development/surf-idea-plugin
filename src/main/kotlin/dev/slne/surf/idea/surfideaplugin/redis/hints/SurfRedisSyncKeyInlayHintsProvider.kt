package dev.slne.surf.idea.surfideaplugin.redis.hints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

class SurfRedisSyncKeyInlayHintsProvider : InlayHintsProvider {

    object Util {
        val SYNC_METHODS = mapOf(
            "createSyncList" to "list:",
            "createSyncSet" to "set:",
            "createSyncMap" to "map:",
            "createSyncValue" to "value:",
        )
    }

    companion object {
        private const val SYNC_NAMESPACE = "surf-redis:sync:"
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (!SurfLibraryDetector.hasSurfRedis(file)) return null
        return Collector()
    }

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            val callExpr = element as? KtCallExpression ?: return
            val module = callExpr.module ?: return
            if (!SurfLibraryDetector.hasSurfRedis(module)) return

            val calleeName = callExpr.calleeExpression?.text ?: return
            val structureNamespace = Util.SYNC_METHODS[calleeName] ?: return

            val extracted = extractIdArgument(callExpr) ?: return
            val expression = extracted.first ?: return
            val isNamed = extracted.second

            val redisKeyPrefix = "$SYNC_NAMESPACE$structureNamespace"
            val argStartOffset = expression.textRange.startOffset

            sink.addPresentation(
                InlineInlayPosition(argStartOffset, relatedToPrevious = false),
                hintFormat = HintFormat.default
            ) {
                if (isNamed) {
                    text(redisKeyPrefix)
                } else {
                    text("id = $redisKeyPrefix")
                }
            }
        }
    }
}

private fun extractIdArgument(callExpr: KtCallExpression): Pair<KtExpression?, Boolean>? {
    val args = callExpr.valueArguments
    if (args.isEmpty()) return null

    val namedIdArg = args.firstOrNull { it.getArgumentName()?.asName?.asString() == "id" }

    val targetArg = namedIdArg
        ?: args.firstOrNull { !it.isNamed() }
        ?: return null

    return targetArg.getArgumentExpression() to (namedIdArg != null)
}