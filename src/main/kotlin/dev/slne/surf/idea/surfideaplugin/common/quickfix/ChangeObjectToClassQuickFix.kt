package dev.slne.surf.idea.surfideaplugin.common.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory

class ChangeObjectToClassQuickFix : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): @IntentionFamilyName String = KotlinBundle.message("fix.change.object.to.class")

    override fun applyFix(
        project: Project,
        element: PsiElement,
        updater: ModPsiUpdater
    ) {
        if (element !is KtObjectDeclaration) return
        val factory = KtPsiFactory(project)
        element.getObjectKeyword()?.replace(factory.createClassKeyword())
    }
}