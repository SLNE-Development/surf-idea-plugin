package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinModCommandQuickFix
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class RedisHandlerReturnTypeInspection : KotlinApplicableInspectionBase.Simple<KtNamedFunction, Unit>() {

    private val handlerAnnotations = setOf(
        SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
    ).map { FqName(it) }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        if (handlerAnnotations.none { KotlinPsiHeuristics.hasAnnotation(element, it) }) return false
        val typeRef = element.typeReference ?: return false
        return typeRef.text != StandardClassIds.Unit.shortClassName.asString()
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { element.typeReference }
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val returnType = element.symbol.returnType
        if (returnType is KaClassType && returnType.isUnitType) return null
        return Unit
    }

    override fun getProblemDescription(
        element: KtNamedFunction,
        context: Unit
    ): @InspectionMessage String {
        val annotationName = handlerAnnotations
            .firstOrNull { KotlinPsiHeuristics.hasAnnotation(element, it) }
            ?.shortName()?.asString() ?: "handler"

        return "Return value of @$annotationName methods is ignored."
    }

    override fun createQuickFix(
        element: KtNamedFunction,
        context: Unit
    ): KotlinModCommandQuickFix<KtNamedFunction> {
        return RemoveReturnTypeQuickFix()
    }

    private class RemoveReturnTypeQuickFix : KotlinModCommandQuickFix<KtNamedFunction>() {
        override fun getFamilyName(): @IntentionFamilyName String = name
        override fun getName(): String = "Remove return type"

        override fun applyFix(
            project: Project,
            element: KtNamedFunction,
            updater: ModPsiUpdater
        ) {
            val typeRef = element.typeReference ?: return
            val colon = element.colon ?: return

            element.deleteChildRange(colon, typeRef)
        }
    }
}