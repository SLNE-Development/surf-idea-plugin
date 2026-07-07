package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.event

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.quickfix.ChangeModifierFix
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

/**
 * Sync events are dispatched synchronously: a `@SurfEventHandler` for a `SurfSyncEvent`
 * must not be `suspend` — registration rejects it. (Handlers for `SurfAsyncEvent` may be.)
 */
class SurfEventHandlerSuspendInspection :
    SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_API_CORE) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (!element.hasModifier(KtTokens.SUSPEND_KEYWORD)) return false
        if (element.valueParameters.size != 1) return false
        return element.hasAnnotationPsi(SurfApiClassNames.SURF_EVENT_HANDLER_ANNOTATION_FQN)
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val eventParameter = element.valueParameters.singleOrNull() ?: return null
        return eventParameter.symbol.returnType
            .isSubtypeOf(SurfApiClassNames.SURF_SYNC_EVENT_CLASS_ID)
            .asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRange.single(element) { it.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtNamedFunction,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "@SurfEventHandler for a SurfSyncEvent must not be suspend",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            ChangeModifierFix.removeModifier(element, KtTokens.SUSPEND_KEYWORD).asQuickFix(),
        )
    }
}
