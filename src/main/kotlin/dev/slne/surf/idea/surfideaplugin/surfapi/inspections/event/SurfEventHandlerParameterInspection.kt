package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.event

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.inspections.event.SurfEventHandlerParameterInspection.Context
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

/**
 * `@SurfEventHandler` functions are registered reflectively and must declare exactly one
 * parameter whose type is a `SurfEvent` subclass; anything else is rejected at registration.
 */
class SurfEventHandlerParameterInspection :
    SurfApplicableInspection<KtNamedFunction, Context>(SurfLibraryMarker.SURF_API_CORE) {

    sealed interface Context {
        data class WrongParameterCount(val actualCount: Int) : Context
        data object WrongParameterType : Context
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        return element.hasAnnotationPsi(SurfApiClassNames.SURF_EVENT_HANDLER_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val valueParameters = element.valueParameters
        if (valueParameters.size != 1) {
            return Context.WrongParameterCount(valueParameters.size)
        }

        val eventParameter = valueParameters.single()
        val isSurfEvent = eventParameter.symbol.returnType.isSubtypeOf(SurfApiClassNames.SURF_EVENT_CLASS_ID)

        return if (!isSurfEvent) Context.WrongParameterType else null
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList ?: it.nameIdentifier ?: it }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount -> buildString {
                append("@SurfEventHandler function must have exactly 1 parameter of type SurfEvent, but found ")
                if (context.actualCount == 0) append("none") else append(context.actualCount)
                append(" parameter(s)")
            }

            Context.WrongParameterType ->
                "Parameter of @SurfEventHandler function must be a subtype of SurfEvent"
        }

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}
