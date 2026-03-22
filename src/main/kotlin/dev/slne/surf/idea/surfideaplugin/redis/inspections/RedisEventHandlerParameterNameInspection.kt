package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.parameterVisitor

class RedisEventHandlerParameterNameInspection : RedisFacetAwareKotlinApplicableInspectionBase<KtParameter, Unit>() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = parameterVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun getApplicableRanges(element: KtParameter): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun KaSession.prepareContext(element: KtParameter): Unit? {
        val function = element.ownerFunction as? KtNamedFunction ?: return null

        val hasAnnotation = function.hasAnnotation(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_ID)
        if (!hasAnnotation) return null

        val isRedisEventSubtype = element.symbol.returnType.isSubtypeOf(SurfRedisClassNames.REDIS_EVENT_CLASS_ID)
        if (!isRedisEventSubtype) return null

        val isCorrectParameterName = element.name == SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME
        if (!isCorrectParameterName) return null

        return Unit
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
            "Parameter of @OnRedisEvent handler should be named '${SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME}'",
            ProblemHighlightType.WEAK_WARNING,
            onTheFly,
            RenameQuickFix(element, SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME)
        )
    }
}