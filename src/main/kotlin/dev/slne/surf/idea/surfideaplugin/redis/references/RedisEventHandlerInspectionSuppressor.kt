package dev.slne.surf.idea.surfideaplugin.redis.references

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList

class RedisEventHandlerInspectionSuppressor : InspectionSuppressor {
    private val handlerAnnotationFqNames = listOf(
        FqName(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION),
        FqName(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION)
    )

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (!(toolId == "UnusedSymbol" || toolId == "unused")) return false

        val parameter = element as? KtParameter ?: return false
        val parameterList = parameter.parent as? KtParameterList ?: return false
        val function = parameterList.parent as? KtNamedFunction ?: return false

        return handlerAnnotationFqNames.any { fqName ->
            KotlinPsiHeuristics.hasAnnotation(function, fqName)
        }
    }

    override fun getSuppressActions(
        element: PsiElement?,
        toolId: String
    ): Array<out SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}