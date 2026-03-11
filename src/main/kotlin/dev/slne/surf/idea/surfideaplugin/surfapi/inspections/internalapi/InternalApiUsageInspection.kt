package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.internalapi

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.service.internalApiService
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.referenceExpressionVisitor

class InternalApiUsageInspection :
    KotlinApplicableInspectionBase<KtNameReferenceExpression, InternalApiUsageInspection.Context>() {
    data class Context(val symbolName: String)

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = referenceExpressionVisitor { element ->
        if (element is KtNameReferenceExpression) {
            visitTargetElement(element, holder, isOnTheFly)
        }
    }

    override fun isApplicableByPsi(element: KtNameReferenceExpression): Boolean {
        val module = element.module ?: return false
        if (!SurfLibraryDetector.hasSurfApiCore(module)) return false
        if (!SurfLibraryDetector.isClassInModuleClasspath(
                module,
                SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION
            )
        ) return false

        return true
    }

    override fun KaSession.prepareContext(element: KtNameReferenceExpression): Context? {
        val symbols = element.mainReference.resolveToSymbols()
        if (symbols.isEmpty()) return null

        val hasHidden = symbols.any { symbol ->
            val declSymbol = symbol as? KaDeclarationSymbol ?: return@any false
            internalApiService().isHiddenInternalApi(declSymbol, element)
        }

        if (!hasHidden) return null

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