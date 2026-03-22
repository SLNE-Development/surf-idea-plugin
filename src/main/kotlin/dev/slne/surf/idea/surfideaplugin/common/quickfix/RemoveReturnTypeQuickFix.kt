package dev.slne.surf.idea.surfideaplugin.common.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinModCommandQuickFix
import org.jetbrains.kotlin.idea.codeinsights.impl.base.CallableReturnTypeUpdaterUtils
import org.jetbrains.kotlin.psi.KtNamedFunction

class RemoveReturnTypeQuickFix : KotlinModCommandQuickFix<KtNamedFunction>() {
    override fun getFamilyName(): @IntentionFamilyName String = name
    override fun getName(): String = "Remove return type"

    override fun applyFix(
        project: Project,
        element: KtNamedFunction,
        updater: ModPsiUpdater
    ) {
        CallableReturnTypeUpdaterUtils.updateType(
            element,
            CallableReturnTypeUpdaterUtils.TypeInfo(CallableReturnTypeUpdaterUtils.TypeInfo.UNIT),
            project,
            updater
        )
    }
}