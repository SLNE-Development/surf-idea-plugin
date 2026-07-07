package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.containingRpcServiceInterfacePsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.propertyVisitor

/**
 * `@RpcService` interfaces describe callable endpoints only — the descriptor generator
 * rejects properties because there is no RPC representation for them.
 */
class RpcServicePropertyInspection :
    SurfApplicableInspection<KtProperty, Unit>(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = propertyVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtProperty): Boolean {
        return element.containingRpcServiceInterfacePsi() != null
    }

    override fun KaSession.prepareContext(element: KtProperty): Unit? {
        val service = element.containingRpcServiceInterfacePsi() ?: return null
        return service.hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID).asUnit
    }

    override fun getApplicableRanges(element: KtProperty): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtProperty,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Properties are not allowed in @RpcService interfaces",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}
