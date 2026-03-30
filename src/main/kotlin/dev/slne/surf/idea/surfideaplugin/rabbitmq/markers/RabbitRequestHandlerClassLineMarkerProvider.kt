package dev.slne.surf.idea.surfideaplugin.rabbitmq.markers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.common.util.isSubClassOf
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.Icon

class RabbitRequestHandlerClassLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Rabbit Request handlers"
    override fun getIcon(): Icon = AllIcons.Providers.RabbitMQ
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun getOptions(): Array<out Option?> {
        return arrayOf(RabbitLineMarkerOptions.requestHandlerOption)
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        if (elements.isEmpty()) return
        if (!RabbitLineMarkerOptions.requestHandlerOption.isEnabled) return

        val first = elements.first()
        if (DumbService.isDumb(first.project)) return

        for (element in elements) {
            val ktClass = element as? KtClass ?: continue
            val isRequest = analyze(ktClass) {
                ktClass.isSubClassOf(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID)
            }

            if (isRequest) {
                collect(ktClass, result)
            }
        }
    }

    private fun collect(ktClass: KtClass, result: MutableCollection<in LineMarkerInfo<*>>) {
        val scope = GlobalSearchScope.moduleWithDependentsScope(ktClass.module ?: return)
        val handlers = findHandlersForType(ktClass, scope)
        if (handlers.isEmpty()) return

        val annotationName = SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_FQN.shortName().asString()
        val builder = NavigationGutterIconBuilder.create(AllIcons.Providers.RabbitMQ)
            .setTargets(handlers)
            .setTooltipText("Go to $annotationName Handlers")
            .setPopupTitle("$annotationName Handlers for ${ktClass.name}")

        result.add(builder.createLineMarkerInfo(ktClass))
    }

    private fun findHandlersForType(
        requestClass: KtClass,
        scope: GlobalSearchScope
    ): List<KtNamedFunction> {
        val requestClassFq = requestClass.fqName?.asString() ?: return emptyList()
        val project = requestClass.project

        val psiFacade = JavaPsiFacade.getInstance(project)
        val annotationClass =
            psiFacade.findClass(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION, GlobalSearchScope.allScope(project))
                ?: return emptyList()

        val handlers = mutableListOf<KtNamedFunction>()
        AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope).forEach { method ->
            val params = method.parameterList.parameters
            if (params.size < 1) return@forEach

            val paramType = params.first().type
            val hasRequestPacketParam = InheritanceUtil.isInheritor(paramType, requestClassFq)

            if (hasRequestPacketParam) {
                val ktFunction = method.navigationElement as? KtNamedFunction
                if (ktFunction != null) {
                    handlers.add(ktFunction)
                }
            }
        }

        return handlers
    }
}