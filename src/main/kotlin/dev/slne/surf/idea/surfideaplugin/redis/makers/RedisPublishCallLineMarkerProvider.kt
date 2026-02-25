package dev.slne.surf.idea.surfideaplugin.redis.makers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.SurfStandardIcons
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import javax.swing.Icon

class RedisPublishCallLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Redis publish/send call handler navigation"
    override fun getIcon(): Icon = SurfStandardIcons.Publisher

    private val publishMethodNames = setOf("publishEvent", "publish")
    private val sendMethodNames = setOf("sendRequest")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        for (element in elements) {
            if (element !is LeafPsiElement || element.elementType != KtTokens.IDENTIFIER) continue

            val nameRef = element.parent as? KtNameReferenceExpression ?: continue
            val callExpr = nameRef.parent as? KtCallExpression ?: continue

            val calleeName = element.text
            val isPublish = calleeName in publishMethodNames
            val isSend = calleeName in sendMethodNames
            if (!isPublish && !isSend) continue

            val module = callExpr.module ?: continue
            if (!SurfLibraryDetector.hasSurfRedis(module)) continue

            val firstArg = callExpr.valueArguments.firstOrNull()?.getArgumentExpression() ?: continue

            val targetClassFqn = org.jetbrains.kotlin.analysis.api.analyze(callExpr) {
                val argType = firstArg.expressionType as? KaClassType ?: return@analyze null
                argType.classId.asSingleFqName().asString()
            } ?: continue

            val project = callExpr.project
            val scope = GlobalSearchScope.moduleWithDependentsScope(module)
            val psiFacade = JavaPsiFacade.getInstance(project)

            val targetClass = psiFacade.findClass(targetClassFqn, scope) ?: continue

            val annotationFqn = if (isPublish) {
                SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION
            } else {
                SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
            }

            val annotationClass = psiFacade.findClass(annotationFqn, GlobalSearchScope.allScope(project))
                ?: continue

            val handlers = findMatchingHandlers(annotationClass, scope, targetClassFqn, isPublish, targetClass)
            if (handlers.isEmpty()) continue

            val label = if (isPublish) {
                "Go to @${SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_SIMPLE} handlers"
            } else {
                "Go to @${SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_SIMPLE} handlers"
            }

            val builder = NavigationGutterIconBuilder.create(icon)
                .setTargets(handlers)
                .setTooltipText(label)
                .setPopupTitle("Handlers for ${targetClass.name}")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun findMatchingHandlers(
        annotationClass: PsiClass,
        scope: GlobalSearchScope,
        targetClassFqn: String,
        isPublish: Boolean,
        targetClass: PsiClass
    ): List<PsiElement> {
        val handlers = mutableListOf<PsiElement>()

        AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope).forEach { method ->
            val params = method.parameterList.parameters
            if (params.size != 1) return@forEach

            val paramType = params[0].type

            val matches = if (isPublish) {
                matchesEventType(paramType, targetClassFqn, targetClass)
            } else {
                matchesRequestType(paramType, targetClassFqn)
            }

            if (matches) {
                handlers.add(method.navigationElement)
            }
        }

        return handlers
    }

    private fun matchesEventType(
        paramType: PsiType,
        targetClassFqn: String,
        targetClass: PsiClass
    ): Boolean {
        val paramClass = (paramType as? PsiClassType)?.resolve() ?: return false
        return paramClass.qualifiedName == targetClassFqn
                || InheritanceUtil.isInheritorOrSelf(targetClass, paramClass, true)
    }

    private fun matchesRequestType(paramType: PsiType, targetClassFqn: String): Boolean {
        val classType = paramType as? PsiClassType ?: return false
        val typeArgs = classType.parameters
        if (typeArgs.size != 1) return false

        val requestArgClass = (typeArgs[0] as? PsiClassType)?.resolve() ?: return false
        return requestArgClass.qualifiedName == targetClassFqn
    }
}