package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.quickfix.ChangeModifierFix
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc.RpcServiceSuspendFunctionInspection.Util.IGNORED_OBJECT_METHODS
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.containingRpcServiceInterfacePsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

/**
 * Every endpoint of a `@RpcService` interface is invoked over the wire and must therefore
 * be declared `suspend` — the KSP descriptor generator rejects blocking functions.
 */
class RpcServiceSuspendFunctionInspection :
    SurfApplicableInspection<KtNamedFunction, Unit>(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = namedFunctionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtNamedFunction): Boolean {
        if (element.hasModifier(KtTokens.SUSPEND_KEYWORD)) return false
        if (element.name in IGNORED_OBJECT_METHODS) return false
        return element.containingRpcServiceInterfacePsi() != null
    }

    override fun KaSession.prepareContext(element: KtNamedFunction): Unit? {
        val service = element.containingRpcServiceInterfacePsi() ?: return null
        return service.hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID).asUnit
    }

    override fun getApplicableRanges(element: KtNamedFunction): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
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
            "Functions in a @RpcService interface must be suspend",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            ChangeModifierFix.addModifier(element, KtTokens.SUSPEND_KEYWORD).asQuickFix(),
        )
    }

    object Util {
        /** Signatures the RPC descriptor generator treats as `Any` members, not endpoints. */
        internal val IGNORED_OBJECT_METHODS = setOf("toString", "equals", "hashCode")
    }
}
