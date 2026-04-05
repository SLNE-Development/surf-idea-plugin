package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.quickfix.RemoveReturnTypeQuickFix
import dev.slne.surf.idea.surfideaplugin.rabbitmq.RabbitFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandler
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.getReturnTypeReference
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RabbitHandlerReturnTypeInspection : RabbitFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, Unit>() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.isRabbitHandlerPsi()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        if (!element.isRabbitHandler()) return null
        return element.returnType.isUnitType.not().asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { element.getReturnTypeReference() }
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
            "Rabbit handler must return Unit",
            ProblemHighlightType.WARNING,
            onTheFly,
            RemoveReturnTypeQuickFix()
        )
    }
}