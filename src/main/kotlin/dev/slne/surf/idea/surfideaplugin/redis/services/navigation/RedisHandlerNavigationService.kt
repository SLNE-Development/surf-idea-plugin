package dev.slne.surf.idea.surfideaplugin.redis.services.navigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass

@Service(Service.Level.PROJECT)
class RedisHandlerNavigationService(
    private val project: Project
) {
    private val javaPsiFacade: JavaPsiFacade
        get() = JavaPsiFacade.getInstance(project)

    fun findHandlersForRedisClass(
        ktClass: KtClass,
        module: Module,
    ): RedisHandlerNavigationTarget? {
        val targetClass = ktClass.toLightClass() ?: return null
        val resolveScope = module.getModuleWithDependenciesAndLibrariesScope(false)

        val kind = findHandlerKindForTargetClass(
            targetClass = targetClass,
            resolveScope = resolveScope,
        ) ?: return null

        val handlerSearchScope = GlobalSearchScope.moduleWithDependentsScope(module)
        val handlers = findHandlersForTargetClass(
            kind = kind,
            targetClass = targetClass,
            handlerSearchScope = handlerSearchScope,
        )

        if (handlers.isEmpty()) return null

        return RedisHandlerNavigationTarget(
            kind = kind,
            targetClass = targetClass,
            handlers = handlers,
        )
    }

    fun findHandlersForCall(
        callExpression: KtCallExpression,
        kind: RedisHandlerKind,
        module: Module,
    ): RedisHandlerNavigationTarget? {
        val targetClassFqn = resolveFirstArgumentClassFqn(callExpression) ?: return null

        val resolveScope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val targetClass = javaPsiFacade.findClass(targetClassFqn, resolveScope) ?: return null

        if (!isTargetClassOfKind(targetClass, kind, resolveScope)) {
            return null
        }

        val handlerSearchScope = GlobalSearchScope.moduleWithDependentsScope(module)
        val handlers = findHandlersForTargetClass(
            kind = kind,
            targetClass = targetClass,
            handlerSearchScope = handlerSearchScope,
        )

        if (handlers.isEmpty()) return null

        return RedisHandlerNavigationTarget(
            kind = kind,
            targetClass = targetClass,
            handlers = handlers,
        )
    }

    private fun resolveFirstArgumentClassFqn(
        callExpression: KtCallExpression,
    ): String? {
        val firstArgument = callExpression.valueArguments
            .firstOrNull()
            ?.getArgumentExpression()
            ?: return null

        return analyze(callExpression) {
            val argumentType = firstArgument.expressionType as? KaClassType
                ?: return@analyze null

            argumentType.classId.asSingleFqName().asString()
        }
    }

    private fun findHandlerKindForTargetClass(
        targetClass: PsiClass,
        resolveScope: GlobalSearchScope,
    ): RedisHandlerKind? {
        return RedisHandlerKind.entries.firstOrNull { kind ->
            isTargetClassOfKind(
                targetClass = targetClass,
                kind = kind,
                resolveScope = resolveScope,
            )
        }
    }

    private fun isTargetClassOfKind(
        targetClass: PsiClass,
        kind: RedisHandlerKind,
        resolveScope: GlobalSearchScope,
    ): Boolean {
        val baseClass = javaPsiFacade.findClass(kind.targetBaseClassFqn, resolveScope)
            ?: return false

        return InheritanceUtil.isInheritorOrSelf(
            targetClass,
            baseClass,
            true,
        )
    }

    private fun findHandlersForTargetClass(
        kind: RedisHandlerKind,
        targetClass: PsiClass,
        handlerSearchScope: GlobalSearchScope,
    ): List<PsiElement> {
        val annotationClass = javaPsiFacade.findClass(
            kind.annotationFqn,
            GlobalSearchScope.allScope(project),
        ) ?: return emptyList()

        val handlers = mutableListOf<PsiElement>()

        AnnotatedElementsSearch.searchPsiMethods(annotationClass, handlerSearchScope)
            .forEach { method ->
                val parameters = method.parameterList.parameters
                if (parameters.size != 1) return@forEach

                val parameterType = parameters.single().type

                if (matchesHandlerParameter(kind, parameterType, targetClass)) {
                    handlers += method.navigationElement
                }
            }

        return handlers.distinct()
    }

    private fun matchesHandlerParameter(
        kind: RedisHandlerKind,
        parameterType: PsiType,
        targetClass: PsiClass,
    ): Boolean {
        return when (kind) {
            RedisHandlerKind.EVENT ->
                matchesEventHandlerParameter(parameterType, targetClass)

            RedisHandlerKind.REQUEST ->
                matchesRequestHandlerParameter(parameterType, targetClass)
        }
    }

    private fun matchesEventHandlerParameter(
        parameterType: PsiType,
        targetClass: PsiClass,
    ): Boolean {
        val parameterClass = (parameterType as? PsiClassType)
            ?.resolve()
            ?: return false

        return InheritanceUtil.isInheritorOrSelf(
            targetClass,
            parameterClass,
            true,
        )
    }

    private fun matchesRequestHandlerParameter(
        parameterType: PsiType,
        targetClass: PsiClass,
    ): Boolean {
        val requestContextType = parameterType as? PsiClassType ?: return false
        val requestContextClass = requestContextType.resolve() ?: return false

        if (!isRequestContextClass(requestContextClass)) {
            return false
        }

        val requestTypeArgument = requestContextType.parameters.singleOrNull() as? PsiClassType
            ?: return false

        val requestArgumentClass = requestTypeArgument.resolve() ?: return false

        return InheritanceUtil.isInheritorOrSelf(
            targetClass,
            requestArgumentClass,
            true,
        )
    }

    private fun isRequestContextClass(
        candidate: PsiClass,
    ): Boolean {
        val requestContextFqn = SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID
            .asSingleFqName()
            .asString()

        if (candidate.qualifiedName == requestContextFqn) {
            return true
        }

        val requestContextClass = javaPsiFacade.findClass(
            requestContextFqn,
            GlobalSearchScope.allScope(project),
        ) ?: return false

        return InheritanceUtil.isInheritorOrSelf(
            candidate,
            requestContextClass,
            true,
        )
    }

    companion object {
        fun getInstance(project: Project): RedisHandlerNavigationService = project.service()
    }
}

fun Project.redisHandlerNavigationService(): RedisHandlerNavigationService =
    RedisHandlerNavigationService.getInstance(this)