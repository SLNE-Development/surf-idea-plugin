@file:Suppress("UnstableApiUsage")

package dev.slne.surf.idea.surfideaplugin.common.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtModifierListOwner

class ChangeModifierFix(
    element: KtModifierListOwner,
    private val removeModifier: KtModifierKeywordToken? = null,
    private val addModifier: KtModifierKeywordToken? = null,
) : PsiUpdateModCommandAction<KtModifierListOwner>(element) {

    override fun getPresentation(context: ActionContext, element: KtModifierListOwner): Presentation? {
        val actionName = when {
            addModifier != null && removeModifier != null ->
                "Change '${removeModifier.value}' to '${addModifier.value}'"

            removeModifier != null ->
                "Remove '${removeModifier.value}' modifier"

            addModifier != null ->
                "Add '${addModifier.value}' modifier"

            else -> return null
        }
        return Presentation.of(actionName)
    }

    override fun getFamilyName(): @IntentionFamilyName String = "Change modifier"

    override fun invoke(context: ActionContext, element: KtModifierListOwner, updater: ModPsiUpdater) {
        addModifier?.let(element::addModifier)
        removeModifier?.let { mod ->
            element.removeModifier(mod)
        }
    }

    companion object {
        fun removeModifier(
            element: KtModifierListOwner,
            modifier: KtModifierKeywordToken
        ): PsiUpdateModCommandAction<KtModifierListOwner> = ChangeModifierFix(element, removeModifier = modifier)

        fun addModifier(
            element: KtModifierListOwner,
            modifier: KtModifierKeywordToken
        ): PsiUpdateModCommandAction<KtModifierListOwner> = ChangeModifierFix(element, addModifier = modifier)
    }
}