package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisEventHandlerParameterInspection :
    KotlinApplicableInspectionBase<KtNamedFunction, RedisEventHandlerParameterInspection.Context>() {
    sealed interface Context {
        data object WrongParameterCount : Context
        data object WrongParameterType : Context
    }

    private val redisEventClassId = ClassId.topLevel(FqName(SurfRedisClassNames.REDIS_EVENT_CLASS))
    private val onRedisEventAnnotation = FqName(SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION)

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        return KotlinPsiHeuristics.hasAnnotation(element, onRedisEventAnnotation)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val valueParameters = element.valueParameters

        if (valueParameters.size != 1) {
            return Context.WrongParameterCount
        }

        val eventParam = valueParameters.first()
        val isSubtype = eventParam.symbol.returnType.isSubtypeOf(redisEventClassId)

        return if (!isSubtype) Context.WrongParameterType else null
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.valueParameterList }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = when (context) {
            is Context.WrongParameterCount ->
                "@OnRedisEvent handler must have exactly 1 parameter (a RedisEvent subtype), " +
                        "found ${element.valueParameters.size}"

            is Context.WrongParameterType ->
                "Parameter of @OnRedisEvent handler must be a subtype of RedisEvent"
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