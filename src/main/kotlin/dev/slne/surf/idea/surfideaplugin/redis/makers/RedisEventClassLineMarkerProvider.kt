package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.redis.services.navigation.redisHandlerNavigationService
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClass
import javax.swing.Icon

class RedisEventClassLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Surf Redis event/request handlers"
    override fun getIcon(): Icon = AllIcons.Gutter.ReadAccess
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val project = elements.firstOrNull()?.project ?: return

        if (DumbService.isDumb(project)) {
            return
        }

        val redisAvailability = mutableMapOf<Module, Boolean>()
        val navigationService = project.redisHandlerNavigationService()

        for (element in elements) {
            if (!element.isKtIdentifier()) continue

            val ktClass = element.parent as? KtClass ?: continue

            if (element != ktClass.nameIdentifier) {
                continue
            }

            val module = ktClass.module ?: continue

            if (!module.hasSurfRedis(redisAvailability)) {
                continue
            }

            val target = navigationService.findHandlersForRedisClass(
                ktClass = ktClass,
                module = module,
            ) ?: continue

            val className = ktClass.name ?: target.targetClass.name ?: "Redis type"

            val builder = NavigationGutterIconBuilder
                .create(icon)
                .setTargets(target.handlers)
                .setTooltipText(buildTooltip(target.kind.annotationSimpleName))
                .setPopupTitle(buildPopupTitle(target.kind.annotationSimpleName, className))

            result += builder.createLineMarkerInfo(element)
        }
    }

    private fun buildTooltip(annotationSimpleName: String): String = buildString {
        append("Go to @")
        append(annotationSimpleName)
        append(" handlers")
    }

    private fun buildPopupTitle(annotationSimpleName: String, className: String): String = buildString {
        append("@")
        append(annotationSimpleName)
        append(" handlers for ")
        append(className)
    }
}