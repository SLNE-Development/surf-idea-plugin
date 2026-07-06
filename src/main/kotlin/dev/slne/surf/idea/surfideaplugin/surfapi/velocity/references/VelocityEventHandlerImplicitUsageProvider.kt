package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.VelocityClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList

class VelocityEventHandlerImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(
        element: PsiElement,
    ): Boolean = when (element) {
        is KtNamedFunction -> isVelocityEventHandlerFunction(element)
        is KtParameter -> isVelocityEventHandlerParameter(element)

        is PsiMethod -> isVelocityEventHandlerMethod(element)
        is PsiParameter -> isVelocityEventHandlerPsiParameter(element)

        else -> false
    }

    override fun isImplicitRead(
        element: PsiElement,
    ): Boolean = when (element) {
        is KtParameter -> isVelocityEventHandlerParameter(element)
        is PsiParameter -> isVelocityEventHandlerPsiParameter(element)
        else -> false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean = false

    private fun isVelocityEventHandlerFunction(function: KtNamedFunction): Boolean {
        return KotlinPsiHeuristics.hasAnnotation(function, VelocityClassNames.SUBSCRIBE_ANNOTATION_FQN)
    }

    private fun isVelocityEventHandlerParameter(
        parameter: KtParameter,
    ): Boolean {
        val parameterList = parameter.parent as? KtParameterList ?: return false
        val function = parameterList.parent as? KtNamedFunction ?: return false

        if (!isVelocityEventHandlerFunction(function)) {
            return false
        }

        return function.valueParameters.size == 1 && function.valueParameters.single() == parameter
    }

    private fun isVelocityEventHandlerMethod(
        method: PsiMethod,
    ): Boolean {
        return AnnotationUtil.isAnnotated(
            method,
            VelocityClassNames.SUBSCRIBE_ANNOTATION,
            0,
        )
    }

    private fun isVelocityEventHandlerPsiParameter(
        parameter: PsiParameter,
    ): Boolean {
        val method = parameter.declarationScope as? PsiMethod ?: return false

        if (!isVelocityEventHandlerMethod(method)) {
            return false
        }

        val parameters = method.parameterList.parameters

        return parameters.size == 1 &&
                parameters.single() == parameter
    }
}