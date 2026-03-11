package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.VelocityClassNames
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtNamedFunction

class VelocityEventHandlerImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean = when (element) {
        is PsiMethod -> AnnotationUtil.isAnnotated(element, VelocityClassNames.SUBSCRIBE_ANNOTATION, 0)
        is KtNamedFunction -> KotlinPsiHeuristics.hasAnnotation(element, VelocityClassNames.SUBSCRIBE_ANNOTATION)
        else -> false
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false
}