package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.Icon

class RedisEventClassLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Redis Event and Request handlers"
    override fun getIcon(): Icon = AllIcons.Gutter.ReadAccess
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        for (element in elements) {
            if (!element.isKtIdentifier()) continue

            val ktClass = element.parent as? KtClass ?: continue
            val module = ktClass.module ?: continue
            if (!SurfLibraryDetector.hasSurfRedis(module)) continue

            val lightClass = ktClass.toLightClass() ?: continue
            val isEvent = InheritanceUtil.isInheritor(lightClass, SurfRedisClassNames.REDIS_EVENT_CLASS)
            val isRequest = InheritanceUtil.isInheritor(lightClass, SurfRedisClassNames.REDIS_REQUEST_CLASS)

            if (!isEvent && !isRequest) continue

            val scope = GlobalSearchScope.moduleWithDependentsScope(module)
            val handlers = findHandlersForType(ktClass, lightClass, isEvent, scope)
            if (handlers.isEmpty()) continue

            val annotationName = if (isEvent) {
                "@${SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_SIMPLE}"
            } else {
                "@${SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_SIMPLE}"
            }

            val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.ReadAccess)
                .setTargets(handlers)
                .setTooltipText("Go to $annotationName Handlers")
                .setPopupTitle("$annotationName Handlers for ${ktClass.name}")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun findHandlersForType(
        eventClass: KtClass,
        lightEventClass: KtLightClass,
        isEvent: Boolean,
        scope: GlobalSearchScope
    ): List<KtNamedFunction> {
        val project = eventClass.project
        val annotationFqn = if (isEvent) {
            SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION
        } else {
            SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
        }

        val psiFacade = JavaPsiFacade.getInstance(project)
        val annotationClass = psiFacade.findClass(annotationFqn, GlobalSearchScope.allScope(project))
            ?: return emptyList()

        val handlers = mutableListOf<KtNamedFunction>()
        AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope).forEach { method ->
            val params = method.parameterList.parameters
            if (params.size != 1) return@forEach

            val paramType = params.first().type
            val matches = if (isEvent) {
                matchesEventType(paramType, lightEventClass)
            } else {
                matchesRequestType(paramType, lightEventClass)
            }

            if (matches) {
                val ktFunction = method.navigationElement as? KtNamedFunction
                if (ktFunction != null) {
                    handlers.add(ktFunction)
                }
            }
        }

        return handlers
    }

    private fun matchesEventType(paramType: PsiType, lightEventClass: KtLightClass): Boolean {
        val paramClass = (paramType as? PsiClassType)?.resolve() ?: return false
        return paramClass == lightEventClass || InheritanceUtil.isInheritorOrSelf(lightEventClass, paramClass, true)
    }

    private fun matchesRequestType(paramType: PsiType, lightRequestClass: KtLightClass): Boolean {
        val classType = paramType as? PsiClassType ?: return false
        val typeArgs = classType.parameters
        if (typeArgs.size != 1) return false

        val requestArgClass = (typeArgs[0] as? PsiClassType)?.resolve() ?: return false
        return requestArgClass == lightRequestClass || InheritanceUtil.isInheritorOrSelf(
            lightRequestClass,
            requestArgClass,
            true
        )
    }

    private fun PsiElement.isKtIdentifier(): Boolean {
        return this is LeafPsiElement && this.elementType == KtTokens.IDENTIFIER
    }
}