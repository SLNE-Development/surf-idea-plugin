package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFixBase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerModifierInspection : KotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    ).map { FqName(it) }

    private val forbiddenModifier = setOf(
        KtTokens.PRIVATE_KEYWORD,
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

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        if (handlerAnnotations.none { KotlinPsiHeuristics.hasAnnotation(element, it) }) return false
        return forbiddenModifier.any { element.hasModifier(it) }
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.multiple(element) {
            forbiddenModifier.mapNotNull { element.modifierList?.getModifier(it) }
        }
    }

    override fun KaSession.prepareContext(element: KtNamedFunction) {
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
                    "Remove modifiers like private/abstract/open/override/inline.",
            ProblemHighlightType.WARNING,
            onTheFly,
            *forbiddenModifier.map {
                RemoveModifierFixBase(
                    element,
                    it,
                    false
                ).asQuickFix()
            }.toTypedArray()
        )
    }
}