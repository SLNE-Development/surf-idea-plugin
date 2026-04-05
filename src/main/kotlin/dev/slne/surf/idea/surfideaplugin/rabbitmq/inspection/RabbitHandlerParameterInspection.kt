package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.rabbitmq.RabbitFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandler
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RabbitHandlerParameterInspection :
    RabbitFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, RabbitHandlerParameterInspection.Context>() {
    sealed interface Context {
        data object WrongParameterCount : Context
        data object WrongParameterType : Context
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.isRabbitHandlerPsi()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        if (!element.isRabbitHandler()) return null

        val valueParameters = element.valueParameters
        if (valueParameters.size != 1) return Context.WrongParameterCount

        val paramType = valueParameters.first().symbol.returnType
        val isRabbitRequest = paramType.isSubtypeOf(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID)
        if (!isRabbitRequest) return Context.WrongParameterType

        return null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitor<*, *> =
        namedFunctionVisitor { element ->
            visitTargetElement(element, holder, isOnTheFly)
        }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount ->
                "@RabbitHandler handler must have exactly 1 parameter (RabbitPacket), " +
                        "found ${element.valueParameters.size}"

            is Context.WrongParameterType ->
                "@RabbitHandler handler parameter must be of type RabbitPacket, " +
                        "found ${element.valueParameters.first().typeReference?.text}"
        }

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            onTheFly
        )
    }
}