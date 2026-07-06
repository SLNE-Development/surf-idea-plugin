package dev.slne.surf.idea.surfideaplugin.surfapi.paper.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.PaperClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList

class PaperEventHandlerImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(
        element: PsiElement,
    ): Boolean {
        return when (element) {
            is KtNamedFunction -> isPaperEventHandlerFunction(element)
            is KtParameter -> isPaperEventHandlerParameter(element)

            is PsiMethod -> isPaperEventHandlerMethod(element)
            is PsiParameter -> isPaperEventHandlerPsiParameter(element)

            else -> false
        }
    }

    override fun isImplicitRead(
        element: PsiElement,
    ): Boolean {
        return element is KtParameter && isPaperEventHandlerParameter(element) ||
                element is PsiParameter && isPaperEventHandlerPsiParameter(element)
    }

    override fun isImplicitWrite(
        element: PsiElement,
    ): Boolean = false

    private fun isPaperEventHandlerFunction(
        function: KtNamedFunction,
    ): Boolean {
        return KotlinPsiHeuristics.hasAnnotation(
            function,
            PaperClassNames.EVENT_HANDLER_ANNOTATION_FQN,
        )
    }

    private fun isPaperEventHandlerParameter(
        parameter: KtParameter,
    ): Boolean {
        val parameterList = parameter.parent as? KtParameterList ?: return false
        val function = parameterList.parent as? KtNamedFunction ?: return false

        if (!isPaperEventHandlerFunction(function)) {
            return false
        }

        return function.valueParameters.size == 1 &&
                function.valueParameters.single() == parameter
    }

    private fun isPaperEventHandlerMethod(
        method: PsiMethod,
    ): Boolean {
        return AnnotationUtil.isAnnotated(
            method,
            PaperClassNames.EVENT_HANDLER_ANNOTATION,
            0,
        )
    }

    private fun isPaperEventHandlerPsiParameter(
        parameter: PsiParameter,
    ): Boolean {
        val method = parameter.declarationScope as? PsiMethod ?: return false

        if (!isPaperEventHandlerMethod(method)) {
            return false
        }

        val parameters = method.parameterList.parameters

        return parameters.size == 1 &&
                parameters.single() == parameter
    }
}