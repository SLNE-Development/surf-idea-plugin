package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisRequestHandlerParameterInspection :
    KotlinApplicableInspectionBase<KtNamedFunction, RedisRequestHandlerParameterInspection.Context>() {
    sealed interface Context {
        data object WrongParameterCount : Context
        data object WrongParameterType : Context
    }

    private val requestContextClassId = ClassId.topLevel(FqName(SurfRedisClassNames.REQUEST_CONTEXT_CLASS))
    private val handleRedisRequestAnnotation = FqName(SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION)
    private val handleRedisRequestAnnotationClassId = ClassId.topLevel(handleRedisRequestAnnotation)

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        return KotlinPsiHeuristics.hasAnnotation(element, handleRedisRequestAnnotation)
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList }
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val hasAnnotation = element.symbol.annotations.any { annotation ->
            annotation.classId == handleRedisRequestAnnotationClassId
        }

        if (!hasAnnotation) return null

        val valueParameters = element.valueParameters
        if (valueParameters.size != 1) return Context.WrongParameterCount

        val paramType = valueParameters.first().symbol.returnType as? KaClassType ?: return Context.WrongParameterType
        if (paramType.classId != requestContextClassId) return Context.WrongParameterType

        return null
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount ->
                "@HandleRedisRequest handler must have exactly 1 parameter (RequestContext<T : RedisRequest>), " +
                        "found ${element.valueParameters.size}"

            is Context.WrongParameterType ->
                "Parameter of @HandleRedisRequest handler must be RequestContext<T : RedisRequest>"
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