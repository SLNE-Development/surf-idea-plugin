package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.common.util.isReturnType
import dev.slne.surf.idea.surfideaplugin.common.util.shortNameString
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.parameterVisitor

/**
 * Verifies the name of the parameter of @[SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION] annotated functions.
 */
class RedisEventHandlerParameterNameInspection :
    SurfApplicableInspection<KtParameter, Unit>(SurfLibraryMarker.SURF_REDIS_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = parameterVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun getApplicableRanges(element: KtParameter): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun isApplicableByPsi(element: KtParameter): Boolean {
        val ownerFunction = element.ownerFunction as? KtNamedFunction ?: return false

        if (!ownerFunction.hasAnnotationPsi(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN)) {
            return false
        }

        return ownerFunction.valueParameters.size == 1 && ownerFunction.valueParameters.single() == element
    }

    override fun KaSession.prepareContext(element: KtParameter): Unit? {
        val isRedisEventSubtype = element.isReturnType(SurfRedisClassNames.REDIS_EVENT_CLASS_ID)
        if (!isRedisEventSubtype) return null

        val isCorrectParameterName = element.name == SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME
        if (isCorrectParameterName) return null

        return Unit
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtParameter,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = buildString {
            append("Parameter of @")
            append(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_FQN.shortNameString())
            append(" handler should be named '${SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME}'")
        }


        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.WEAK_WARNING,
            onTheFly,
            RenameQuickFix(element, SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME)
        )
    }
}