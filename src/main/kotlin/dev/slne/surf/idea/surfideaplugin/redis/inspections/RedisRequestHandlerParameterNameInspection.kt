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
import dev.slne.surf.idea.surfideaplugin.common.util.shortNameString
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.parameterVisitor

class RedisRequestHandlerParameterNameInspection :
    SurfApplicableInspection<KtParameter, Unit>(SurfLibraryMarker.SURF_REDIS_API) {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = parameterVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(
        element: KtParameter,
    ): Boolean {
        if (element.nameIdentifier == null) return false

        val function = element.ownerFunction as? KtNamedFunction ?: return false

        if (!function.hasAnnotationPsi(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN)) {
            return false
        }

        if (function.valueParameters.size != 1) {
            return false
        }

        if (function.valueParameters.single() != element) {
            return false
        }

        return element.name != SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME
    }

    override fun KaSession.prepareContext(element: KtParameter): Unit? {
        val paramType = element.symbol.returnType as? KaClassType ?: return null
        if (paramType.classId != SurfRedisClassNames.REQUEST_CONTEXT_CLASS_ID) return null

        return Unit
    }

    override fun getApplicableRanges(element: KtParameter): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtParameter,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = buildString {
            append("RequestContext parameter of @")
            append(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_FQN.shortNameString())
            append(" handler should be named '")
            append(SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME)
            append("'")
        }

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.WEAK_WARNING,
            onTheFly,
            RenameQuickFix(element, SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME)
        )
    }
}