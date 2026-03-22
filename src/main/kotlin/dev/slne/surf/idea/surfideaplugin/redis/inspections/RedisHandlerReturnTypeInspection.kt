package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.quickfix.RemoveReturnTypeQuickFix
import dev.slne.surf.idea.surfideaplugin.redis.RedisFacetAwareKotlinApplicableInspectionBase
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerReturnTypeInspection : RedisFacetAwareKotlinApplicableInspectionBase<KtNamedFunction, String>() {
    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_ID,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_ID
    )

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        val typeRef = element.typeReference ?: return false
        return typeRef.text != StandardClassIds.Unit.shortClassName.asString()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): String? {
        val handlerAnnotation = element.symbol.annotations.find { it.classId in handlerAnnotations }
        if (handlerAnnotation == null) return null

        val isIncorrectReturnType = element.symbol.returnType.isUnitType
        if (!isIncorrectReturnType) return null

        val shortHandlerName = handlerAnnotation.classId?.shortClassName?.asString() ?: "handler"
        return shortHandlerName
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { element.typeReference }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: String,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Return value of @$context handler is ignored.",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            onTheFly,
            RemoveReturnTypeQuickFix()
        )
    }
}