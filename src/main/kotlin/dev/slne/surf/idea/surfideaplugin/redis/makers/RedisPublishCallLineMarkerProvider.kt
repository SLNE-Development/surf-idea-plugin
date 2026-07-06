package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.SurfStandardIcons
import dev.slne.surf.idea.surfideaplugin.redis.services.navigation.RedisHandlerKind
import dev.slne.surf.idea.surfideaplugin.redis.services.navigation.redisHandlerNavigationService
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import javax.swing.Icon

class RedisPublishCallLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Surf Redis publish/send handler navigation"
    override fun getIcon(): Icon = SurfStandardIcons.Publisher

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val project = elements.firstOrNull()?.project ?: return

        if (DumbService.isDumb(project)) {
            return
        }

        val redisAvailability = mutableMapOf<Module, Boolean>()
        val navigationService = project.redisHandlerNavigationService()

        for (element in elements) {
            if (!element.isKtIdentifier()) continue

            val nameReference = element.parent as? KtNameReferenceExpression ?: continue
            val callExpression = nameReference.parent as? KtCallExpression ?: continue

            if (callExpression.calleeExpression != nameReference) {
                continue
            }

            val kind = RedisHandlerKind.fromCallName(nameReference.getReferencedName()) ?: continue

            val module = callExpression.module ?: continue

            if (!module.hasSurfRedis(redisAvailability)) {
                continue
            }

            val target = navigationService.findHandlersForCall(
                callExpression = callExpression,
                kind = kind,
                module = module,
            ) ?: continue

            val targetName = target.targetClass.name ?: "Redis target"

            val builder = NavigationGutterIconBuilder
                .create(icon)
                .setTargets(target.handlers)
                .setTooltipText(buildTooltip(target.kind.annotationSimpleName))
                .setPopupTitle(buildPopupTitle(targetName))

            result += builder.createLineMarkerInfo(element)
        }
    }

    private fun buildTooltip(annotationSimpleName: String): String = buildString {
        append("Go to @")
        append(annotationSimpleName)
        append(" handlers")
    }

    private fun buildPopupTitle(targetName: String): String = buildString {
        append("Handlers for ")
        append(targetName)
    }
}