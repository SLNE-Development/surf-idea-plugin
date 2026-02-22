package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.java.ultimate.icons.JavaUltimateIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction

class RedisEventHandlerLineMarkerProvider : LineMarkerProvider {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    ).map { FqName(it) }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val function = element.parent as? KtNamedFunction ?: return null
        if (element != function.nameIdentifier) return null

        val handler = handlerAnnotations.firstNotNullOfOrNull {
            KotlinPsiHeuristics.findAnnotation(function, it)
        }

        if (handler == null) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            JavaUltimateIcons.Cdi.Listener,
            { "Surf Redis: @${handler.shortName?.asString()} handler" },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "Surf Redis handler" }
        )
    }
}