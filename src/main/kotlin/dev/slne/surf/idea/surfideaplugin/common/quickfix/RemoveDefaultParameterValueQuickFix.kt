package dev.slne.surf.idea.surfideaplugin.common.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameter

class RemoveDefaultParameterValueQuickFix : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): @IntentionFamilyName String = "Remove default parameter value"

    override fun applyFix(
        project: Project,
        element: PsiElement,
        updater: ModPsiUpdater
    ) {
        if (element !is KtParameter) return
        val defaultValue = element.defaultValue ?: return
        val equalsToken = element.equalsToken

        element.deleteChildRange(equalsToken ?: defaultValue, defaultValue)
    }
}
