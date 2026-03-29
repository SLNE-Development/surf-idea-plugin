package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.SurfCommonClassNames
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.RabbitFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classVisitor

class RabbitPacketIsNotSerializableInspection : RabbitFacetAwareKotlinApplicableInspectionBase<KtClass, Unit>() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    @Suppress("DEPRECATION")
    override fun KaSession.prepareContext(element: KtClass): Unit? {
        val isRabbitPacket = element.returnType.isSubtypeOf(SurfRabbitClassNames.RABBIT_PACKET_CLASS_ID)
        if (!isRabbitPacket) return null

        val isSerializable = element.hasAnnotation(SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_ID)
        if (isSerializable) return null

        return Unit
    }

    override fun getApplicableRanges(element: KtClass): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtClass,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Rabbit packet must be annotated with '@Serializable'",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            AddAnnotationFix(element, SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_ID).asQuickFix()
        )
    }
}