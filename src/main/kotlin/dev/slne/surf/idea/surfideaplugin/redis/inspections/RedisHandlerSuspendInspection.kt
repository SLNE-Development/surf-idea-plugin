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

class RedisHandlerSuspendInspection : RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_ID,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID
    )

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val hasAnnotation = element.hasAnyAnnotation(handlerAnnotations)
        if (!hasAnnotation) return null

        val isSuspend = element.hasModifier(KtTokens.SUSPEND_KEYWORD)
        if (!isSuspend) return null

        return Unit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) ?: it }
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
            "Redis handler '${element.name}' must not be a suspend function. " +
                    "Use your own coroutineScope.launch {} for async work instead.",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            ChangeModifierFix.removeModifier(element, KtTokens.SUSPEND_KEYWORD).asQuickFix()
        )
    }
}