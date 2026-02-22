package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid

class RedisHandlerSuspendInspection : AbstractKotlinInspection() {

    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    )

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = object : KtVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            val module = function.module ?: return
            if (!SurfLibraryDetector.hasSurfRedis(module)) return

            val hasHandlerAnnotation = function.toLightMethods().any {
                AnnotationUtil.isAnnotated(it, handlerAnnotations, 0)
            }

            if (!hasHandlerAnnotation) return
            val suspendModifier = function.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD)

            if (suspendModifier != null) {
                holder.registerProblem(
                    suspendModifier,
                    "Redis handler '${function.name}' must not be a suspend function. " +
                            "Use your own coroutineScope.launch {} for async work instead.",
                    ProblemHighlightType.GENERIC_ERROR,
                    RemoveSuspendQuickFix()
                )
            }
        }
    }

    private class RemoveSuspendQuickFix : LocalQuickFix {
        override fun getName(): String = "Remove 'suspend' modifier"
        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement.parent?.parent as? KtNamedFunction ?: return
            function.removeModifier(KtTokens.SUSPEND_KEYWORD)
        }
    }
}