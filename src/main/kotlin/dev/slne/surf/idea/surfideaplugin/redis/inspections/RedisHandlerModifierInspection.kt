package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.quickfix.ChangeModifierFix
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnyAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerModifierInspection : RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_ID,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID
    )

    private val forbiddenModifier = setOf(
        KtTokens.ABSTRACT_KEYWORD,
        KtTokens.OPEN_KEYWORD,
        KtTokens.OVERRIDE_KEYWORD,
        KtTokens.INLINE_KEYWORD,
    )

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val isHandler = element.hasAnyAnnotation(handlerAnnotations)
        if (!isHandler) return null

        val hasForbiddenModifier = forbiddenModifier.any { element.hasModifier(it) }
        if (!hasForbiddenModifier) return null

        return Unit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.multiple(element) {
            forbiddenModifier.mapNotNull { element.modifierList?.getModifier(it) }
        }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Redis handler '${element.name}' must be accessible for MethodHandles.Lookup. " +
                    "Remove modifiers like ${forbiddenModifier.joinToString("/", transform = { it.value })}.",
            ProblemHighlightType.WARNING,
            onTheFly,
            *forbiddenModifier.map {
                ChangeModifierFix.removeModifier(element, it).asQuickFix()
            }.toTypedArray()
        )
    }
}