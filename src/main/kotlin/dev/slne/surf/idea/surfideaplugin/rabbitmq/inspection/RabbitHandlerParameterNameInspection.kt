package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RabbitHandlerParameterNameInspection : SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_RABBITMQ_SERVER_API) {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        val parameters = element.valueParameters
        if (parameters.isEmpty()) return false
        if (parameters.first().name == SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME) return false

        return element.hasAnnotationPsi(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        return element.hasAnnotation(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_ID).asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameters.firstOrNull()?.nameIdentifier }
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
            "Parameter of @RabbitHandler should be named '${SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME}'",
            ProblemHighlightType.WEAK_WARNING,
            onTheFly,
            RenameQuickFix(element.valueParameters.first(), SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME)
        )
    }
}