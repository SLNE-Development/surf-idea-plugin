package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfKotlinInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnyAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.ChangeModifiersFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerModifierInspection : SurfKotlinInspection(SurfLibraryMarker.SURF_REDIS_API) {

    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN
    )

    private val forbiddenModifiers = setOf(
        KtTokens.ABSTRACT_KEYWORD,
        KtTokens.OPEN_KEYWORD,
        KtTokens.OVERRIDE_KEYWORD,
        KtTokens.INLINE_KEYWORD,
    )

    private val forbiddenModifiersJoined = forbiddenModifiers.joinToString("/") { it.value }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor(fun(function) {
        if (!function.hasAnyAnnotationPsi(handlerAnnotations)) return

        val modifierList = function.modifierList ?: return
        val presentForbiddenModifiers = forbiddenModifiers
            .filter { modifierList.hasModifier(it) }

        if (presentForbiddenModifiers.isEmpty()) return

        for (modifier in presentForbiddenModifiers) {
            val modifierElement = modifierList.getModifier(modifier) ?: continue

            holder.registerProblem(
                modifierElement,
                buildMessage(function),
                ProblemHighlightType.WARNING,
                ChangeModifiersFix.removeModifierFix(function, modifier).asQuickFix(),
            )
        }
    })

    private fun buildMessage(function: KtNamedFunction): String {
        val functionName = function.name ?: "<anonymous>"

        return "Redis handler '$functionName' must be accessible for MethodHandles.Lookup. " +
                "Remove modifiers like $forbiddenModifiersJoined."
    }
}