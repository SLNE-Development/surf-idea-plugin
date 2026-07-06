package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.internalapi

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.surfapi.inspections.internalapi.InternalApiUsageInspection.Context
import dev.slne.surf.idea.surfideaplugin.surfapi.service.internalApiService
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.referenceExpressionVisitor

class InternalApiUsageInspection :
    SurfApplicableInspection<KtNameReferenceExpression, Context>(SurfLibraryMarker.SURF_API_CORE) {

    data class Context(val symbolName: String)

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = referenceExpressionVisitor { element ->
        if (element is KtNameReferenceExpression) {
            visitTargetElement(element, holder, isOnTheFly)
        }
    }

    override fun isSurfApplicableByPsi(element: KtNameReferenceExpression): Boolean {
        return element.getReferencedName().isNotBlank()
    }

    override fun KaSession.prepareContext(element: KtNameReferenceExpression): Context? {
        val internalApiService = element.project.internalApiService()

        val hasHiddenInternalApi = element.mainReference
            .resolveToSymbols()
            .asSequence()
            .filterIsInstance<KaDeclarationSymbol>()
            .any { symbol ->
                internalApiService.isHiddenInternalApi(
                    symbol = symbol,
                    useSiteElement = element,
                )
            }

        if (!hasHiddenInternalApi) {
            return null
        }

        return Context(element.getReferencedName())
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNameReferenceExpression,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Cannot access '${context.symbolName}': it is internal API from a library dependency",
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
            onTheFly
        )
    }
}