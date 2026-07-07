package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.Icon

class RedisEventHandlerLineMarkerProvider : LineMarkerProviderDescriptor() {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN
    )

    override fun getName(): @GutterName String {
        return "Surf Redis handler"
    }

    override fun getIcon(): Icon {
        return AllIcons.Gutter.ExtAnnotation
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val function = element.parent as? KtNamedFunction ?: return null
        if (element != function.nameIdentifier) return null

        val annotation = handlerAnnotations.firstNotNullOfOrNull { annotationFqName ->
            KotlinPsiHeuristics.findAnnotation(function, annotationFqName)
        } ?: return null

        val annotationName = annotation.shortName?.asString() ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { buildTooltip(annotationName) },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "Surf Redis handler" }
        )
    }

    private fun buildTooltip(annotationName: String): String = buildString {
        append("Surf Redis: @")
        append(annotationName)
        append(" handler")
    }
}