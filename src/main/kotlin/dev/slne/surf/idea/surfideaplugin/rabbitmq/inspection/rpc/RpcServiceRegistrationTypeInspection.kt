package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc.RpcServiceRegistrationTypeInspection.Context
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.callExpressionVisitor

private val RPC_SERVICE_FACTORY_FUNCTIONS = setOf(
    "registerRpcService",
    "unregisterRpcService",
    "createRpcService",
)

private val RABBITMQ_API_PACKAGE = FqName("dev.slne.surf.rabbitmq.api")

class RpcServiceRegistrationTypeInspection :
    SurfApplicableInspection<KtCallExpression, Context>(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    sealed interface Context {
        data class NotAnInterface(val typeName: String, val candidateInterfaceFqn: String?) : Context
        data class NotAnnotated(val typeName: String, val candidateInterfaceFqn: String?) : Context
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = callExpressionVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtCallExpression): Boolean {
        val callee = element.calleeExpression as? KtNameReferenceExpression ?: return false
        return callee.getReferencedName() in RPC_SERVICE_FACTORY_FUNCTIONS
    }

    override fun KaSession.prepareContext(element: KtCallExpression): Context? {
        val resolvedCall = element.resolveToCall()?.successfulFunctionCallOrNull() ?: return null

        val callableId = resolvedCall.symbol.callableId ?: return null
        if (callableId.callableName.asString() !in RPC_SERVICE_FACTORY_FUNCTIONS) return null
        if (callableId.packageName != RABBITMQ_API_PACKAGE) return null

        val serviceType = resolvedCall.typeArgumentsMapping.values.firstOrNull() as? KaClassType ?: return null
        val serviceClass = serviceType.symbol as? KaClassSymbol ?: return null

        if (serviceClass.isRpcServiceInterface()) return null

        val typeName = serviceClass.name?.asString() ?: return null
        val candidateFqn = serviceType.allSupertypes
            .mapNotNull { (it as? KaClassType)?.symbol as? KaClassSymbol }
            .firstOrNull { it.isRpcServiceInterface() }
            ?.classId
            ?.asFqNameString()

        return if (serviceClass.classKind == KaClassKind.INTERFACE) {
            Context.NotAnnotated(typeName, candidateFqn)
        } else {
            Context.NotAnInterface(typeName, candidateFqn)
        }
    }

    private fun KaClassSymbol.isRpcServiceInterface(): Boolean {
        return classKind == KaClassKind.INTERFACE &&
                annotations.any { it.classId == SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID }
    }

    override fun getApplicableRanges(element: KtCallExpression): List<TextRange> {
        return ApplicabilityRange.single(element) { it.typeArgumentList ?: it.calleeExpression ?: it }
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtCallExpression,
        context: Context,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val callName = (element.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
            ?: "registerRpcService"

        val (message, candidateFqn) = when (context) {
            is Context.NotAnInterface -> buildString {
                append("The service type of '")
                append(callName)
                append("' must be the @RpcService interface, not the implementation '")
                append(context.typeName)
                append("'")
                context.candidateInterfaceFqn?.let { fqn ->
                    append(" — use ")
                    append(callName)
                    append("<")
                    append(FqName(fqn).shortName().asString())
                    append(">(...)")
                }
            } to context.candidateInterfaceFqn

            is Context.NotAnnotated -> buildString {
                append("'")
                append(context.typeName)
                append("' is not annotated with @RpcService — '")
                append(callName)
                append("' requires the annotated service interface as type argument")
            } to context.candidateInterfaceFqn
        }

        val fixes: Array<LocalQuickFix> = candidateFqn
            ?.let { arrayOf(SpecifyRpcServiceTypeArgumentQuickFix(it)) }
            ?: emptyArray()

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            *fixes,
        )
    }
}