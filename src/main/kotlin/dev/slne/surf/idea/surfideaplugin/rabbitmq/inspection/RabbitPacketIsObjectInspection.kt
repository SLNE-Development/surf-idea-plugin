package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.rabbitmq.RabbitFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.ChangeObjectToClassFix
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classOrObjectVisitor

class RabbitPacketIsObjectInspection : RabbitFacetAwareKotlinApplicableInspectionBase<KtObjectDeclaration, Unit>() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classOrObjectVisitor { element ->
        if (element is KtObjectDeclaration) {
            visitTargetElement(element, holder, isOnTheFly)
        }
    }

    @Suppress("DEPRECATION")
    override fun KaSession.prepareContext(element: KtObjectDeclaration): Unit? {
        val isRabbitPacket = element.returnType.isSubtypeOf(SurfRabbitClassNames.RABBIT_PACKET_CLASS_ID)
        if (!isRabbitPacket) return null

        return Unit
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtObjectDeclaration,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Use a class instead of an object for Rabbit packet",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            ChangeObjectToClassFix(element).asQuickFix()
        )
    }
}