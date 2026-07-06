package dev.slne.surf.idea.surfideaplugin.redis.services.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

data class RedisHandlerNavigationTarget(
    val kind: RedisHandlerKind,
    val targetClass: PsiClass,
    val handlers: List<PsiElement>,
)
