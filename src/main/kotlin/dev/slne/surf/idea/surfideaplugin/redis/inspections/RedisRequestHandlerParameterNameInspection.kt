package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.parameterVisitor

class RedisRequestHandlerParameterNameInspection : KotlinApplicableInspectionBase<KtParameter, Unit>() {
    private val handleRedisRequestAnnotation = FqName(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION)
    private val handleRedisRequestAnnotationClassId = ClassId.topLevel(handleRedisRequestAnnotation)
    private val requestContextClassId = ClassId.topLevel(FqName(SurfRedisClassNames.REQUEST_CONTEXT_CLASS))

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = parameterVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtParameter): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        val function = element.ownerFunction as? KtNamedFunction ?: return false
        if (!KotlinPsiHeuristics.hasAnnotation(function, handleRedisRequestAnnotation)) return false
        return element.name != SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME
    }

    override fun getApplicableRanges(element: KtParameter): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun KaSession.prepareContext(element: KtParameter): Unit? {
        val function = element.ownerFunction as? KtNamedFunction ?: return null
        val hasAnnotation = function.symbol.annotations.any { annotation ->
            annotation.classId == handleRedisRequestAnnotationClassId
        }
        if (!hasAnnotation) return null

        val paramType = element.symbol.returnType as? KaClassType ?: return null
        if (paramType.classId != requestContextClassId) return null

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
            "RequestContext parameter should be named '${SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME}'",
            ProblemHighlightType.WEAK_WARNING,
            onTheFly,
            RenameQuickFix(element, SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME)
        )
    }
}