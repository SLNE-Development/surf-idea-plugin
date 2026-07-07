package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.quickfix.RemoveReturnTypeQuickFix
import dev.slne.surf.idea.surfideaplugin.common.util.findFirstAnnotation
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.inspections.RedisHandlerReturnTypeInspection.Context
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerReturnTypeInspection :
    SurfApplicableInspection<KtNamedFunction, Context>(SurfLibraryMarker.SURF_REDIS_API) {

    data class Context(val handlerName: String)

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
        val typeReference = element.typeReference ?: return false
        return typeReference.text != StandardClassIds.Unit.shortClassName.asString()
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Context? {
        val handlerAnnotation = element.findFirstAnnotation(handlerAnnotations) ?: return null

        if (element.symbol.returnType.isUnitType) return null

        val handlerName = handlerAnnotation.classId
            ?.shortClassName
            ?.asString()
            ?: "handler"

        return Context(handlerName)
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { element.typeReference }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "@${context.handlerName} handler must return Unit",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            onTheFly,
            RemoveReturnTypeQuickFix()
        )
    }
}