package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.quickfix.ChangeModifierFix
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandler
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RabbitHandlerModifierInspection : SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_RABBITMQ_SERVER_API) {
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

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        val hasForbiddenModifier = forbiddenModifier.any { element.hasModifier(it) }
        if (!hasForbiddenModifier) return false

        return element.isRabbitHandlerPsi()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        return element.isRabbitHandler().asUnit
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
            "Rabbit handler must be accessible for MethodHandles.Lookup. " +
                    "Remove modifiers like ${forbiddenModifier.joinToString("/", transform = { it.value })}.",
            ProblemHighlightType.WARNING,
            onTheFly,
            *forbiddenModifier.map {
                ChangeModifierFix.removeModifier(element, it).asQuickFix()
            }.toTypedArray()
        )
    }
}