package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRpcServicePsi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classOrObjectVisitor

/**
 * `@RpcService` marks the RPC contract that the KSP processor generates a descriptor and
 * client implementation for. The processor rejects anything that is not an interface.
 */
class RpcServiceMustBeInterfaceInspection :
    SurfApplicableInspection<KtClassOrObject, Unit>(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classOrObjectVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtClassOrObject): Boolean {
        if (element is KtClass && element.isInterface()) return false
        return element.isRpcServicePsi()
    }

    override fun KaSession.prepareContext(element: KtClassOrObject): Unit? {
        return element.hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID).asUnit
    }

    override fun getApplicableRanges(element: KtClassOrObject): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtClassOrObject,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "@RpcService is only applicable to interfaces",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}
