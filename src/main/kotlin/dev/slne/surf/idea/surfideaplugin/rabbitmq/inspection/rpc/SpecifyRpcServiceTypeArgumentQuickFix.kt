@file:OptIn(KaIdeApi::class)

package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferences
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeArgumentList

class SpecifyRpcServiceTypeArgumentQuickFix(
    private val interfaceFqn: String,
) : PsiUpdateModCommandQuickFix() {

    override fun getFamilyName(): @IntentionFamilyName String =
        "Specify @RpcService interface as type argument"

    override fun applyFix(
        project: Project,
        element: PsiElement,
        updater: ModPsiUpdater
    ) {
        val call = element as? KtCallExpression ?: return
        val newTypeArguments = KtPsiFactory(project).createTypeArguments("<$interfaceFqn>")

        val existing = call.typeArgumentList
        val inserted = if (existing != null) {
            existing.replace(newTypeArguments)
        } else {
            val callee = call.calleeExpression ?: return
            call.addAfter(newTypeArguments, callee)
        }

        shortenReferences(inserted as KtTypeArgumentList)
    }
}
