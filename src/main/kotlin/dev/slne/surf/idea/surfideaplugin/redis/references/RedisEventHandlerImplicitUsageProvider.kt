package dev.slne.surf.idea.surfideaplugin.redis.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction

class RedisEventHandlerImplicitUsageProvider : ImplicitUsageProvider {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    )
    private val handlerAnnotationFqNames = handlerAnnotations.map { FqName(it) }

    override fun isImplicitUsage(element: PsiElement): Boolean {
        return isAnnotatedWithHandler(element)
    }

    private fun isAnnotatedWithHandler(element: PsiElement?): Boolean = when (element) {
        is PsiMethod -> AnnotationUtil.isAnnotated(element, handlerAnnotations, 0)
        is KtNamedFunction -> handlerAnnotationFqNames.any { KotlinPsiHeuristics.hasAnnotation(element, it) }
        else -> false
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false
}