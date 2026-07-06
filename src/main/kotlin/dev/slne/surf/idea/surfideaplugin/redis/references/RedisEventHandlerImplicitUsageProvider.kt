package dev.slne.surf.idea.surfideaplugin.redis.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnyAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.references.RedisEventHandlerImplicitUsageProvider.Util.HANDLER_ANNOTATION
import dev.slne.surf.idea.surfideaplugin.util.FqClassNameSet
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList

class RedisEventHandlerImplicitUsageProvider : ImplicitUsageProvider {
    object Util {
        val HANDLER_ANNOTATION = FqClassNameSet(
            SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
            SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION,
        )
    }

    override fun isImplicitUsage(element: PsiElement): Boolean = when (element) {
        is KtNamedFunction -> isRedisHandlerFunction(element)
        is KtParameter -> isRedisHandlerParameter(element)

        is PsiMethod -> isRedisHandlerMethod(element)
        is PsiParameter -> isRedisHandlerPsiParameter(element)

        else -> false
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false

    override fun isImplicitWrite(element: PsiElement): Boolean = false

    private fun isRedisHandlerFunction(function: KtNamedFunction): Boolean {
        return function.hasAnyAnnotationPsi(HANDLER_ANNOTATION)
    }

    private fun isRedisHandlerParameter(parameter: KtParameter): Boolean {
        val parameterList = parameter.parent as? KtParameterList ?: return false
        val function = parameterList.parent as? KtNamedFunction ?: return false

        if (!isRedisHandlerFunction(function)) {
            return false
        }

        return function.valueParameters.size == 1 && function.valueParameters.single() == parameter
    }

    private fun isRedisHandlerMethod(method: PsiMethod): Boolean {
        return AnnotationUtil.isAnnotated(
            method,
            HANDLER_ANNOTATION.classNames,
            AnnotationUtil.CHECK_HIERARCHY,
        )
    }

    private fun isRedisHandlerPsiParameter(parameter: PsiParameter): Boolean {
        val method = parameter.declarationScope as? PsiMethod ?: return false

        if (!isRedisHandlerMethod(method)) {
            return false
        }

        val parameters = method.parameterList.parameters

        return parameters.size == 1 && parameters.single() == parameter
    }
}