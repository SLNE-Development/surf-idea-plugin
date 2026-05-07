package dev.slne.surf.idea.surfideaplugin.rabbitmq.references

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import org.jetbrains.kotlin.psi.KtNamedFunction

class RabbitHandlerImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element !is KtNamedFunction) return false
        return element.isRabbitHandlerPsi()
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false
}