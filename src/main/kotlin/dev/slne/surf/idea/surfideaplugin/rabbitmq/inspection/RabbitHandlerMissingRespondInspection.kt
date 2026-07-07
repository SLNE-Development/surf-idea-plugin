package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandler
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitRequestPacket
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.*

class RabbitHandlerMissingRespondInspection : SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_RABBITMQ_SERVER_API) {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.hasBlockBody() && element.isRabbitHandlerPsi()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        if (!element.isRabbitHandler()) return null

        val requestParam = element.valueParameters.firstOrNull() ?: return null
        if (!requestParam.isRabbitRequestPacket()) return null
        val requestPacketName = requestParam.name ?: return null

        var foundRespondCall = false
        element.bodyExpression?.accept(callExpressionRecursiveVisitor(fun(element) {
            if (foundRespondCall) return

            val expression = element.calleeExpression ?: return
            if (expression.text != SurfRabbitConstants.RABBIT_RESPONSE_METHOD_NAME) return

            val parent = element.parent as? KtDotQualifiedExpression ?: return
            val receiver = parent.receiverExpression as? KtNameReferenceExpression ?: return

            if (receiver.getReferencedName() == requestPacketName) {
                foundRespondCall = true
            }
        }))

        return foundRespondCall.not().asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
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
            "@RabbitHandler handler '${element.name}' must call '${SurfRabbitConstants.RABBIT_RESPONSE_METHOD_NAME}(...)' on the RabbitPacket",
            ProblemHighlightType.WARNING,
            onTheFly,
        )
    }
}