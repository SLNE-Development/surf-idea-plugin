package dev.slne.surf.idea.surfideaplugin.common.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier

fun PsiClass.isConcreteClass(): Boolean {
    return !isInterface
            && !isEnum
            && !isAnnotationType
            && !hasModifierProperty(PsiModifier.ABSTRACT)
}