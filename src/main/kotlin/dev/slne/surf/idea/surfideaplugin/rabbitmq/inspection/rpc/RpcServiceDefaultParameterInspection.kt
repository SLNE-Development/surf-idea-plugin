package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.quickfix.RemoveDefaultParameterValueQuickFix
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.containingRpcServiceInterfacePsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.parameterVisitor

/**
 * The RPC runtime always transmits the full argument array; a default value on a
 * `@RpcService` endpoint parameter is never applied on the server and only suggests
 * optionality that does not exist on the wire.
 */
class RpcServiceDefaultParameterInspection :
    SurfApplicableInspection<KtParameter, Unit>(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = parameterVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtParameter): Boolean {
        if (!element.hasDefaultValue()) return false
        val function = element.ownerFunction as? KtNamedFunction ?: return false
        return function.containingRpcServiceInterfacePsi() != null
    }

    override fun KaSession.prepareContext(element: KtParameter): Unit? {
        val function = element.ownerFunction as? KtNamedFunction ?: return null
        val service = function.containingRpcServiceInterfacePsi() ?: return null
        return service.hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID).asUnit
    }

    override fun getApplicableRanges(element: KtParameter): List<TextRange> {
        return ApplicabilityRange.single(element) { it.defaultValue }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtParameter,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Default parameter values are not applied by the RPC runtime — the client always sends every argument",
            ProblemHighlightType.WARNING,
            onTheFly,
            RemoveDefaultParameterValueQuickFix(),
        )
    }
}
