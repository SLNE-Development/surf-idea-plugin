package dev.slne.surf.idea.surfideaplugin.redis.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames

class RedisEventHandlerImplicitUsageProvider : ImplicitUsageProvider {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    )

    override fun isImplicitUsage(element: PsiElement): Boolean {
        return element is PsiMethod && AnnotationUtil.isAnnotated(element, handlerAnnotations, 0)
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false
}