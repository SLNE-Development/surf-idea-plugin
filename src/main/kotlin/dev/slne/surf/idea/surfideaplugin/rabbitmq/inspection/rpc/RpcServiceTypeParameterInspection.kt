package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfKotlinInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.containingRpcServiceInterfacePsi
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRpcServicePsi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtVisitorVoid

/**
 * The RPC descriptor generator cannot represent generic services or generic endpoint
 * functions: type parameters are rejected on `@RpcService` interfaces and their functions.
 */
class RpcServiceTypeParameterInspection : SurfKotlinInspection(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = object : KtVisitorVoid() {

        override fun visitClass(klass: KtClass) {
            if (!klass.isInterface()) return
            if (klass.typeParameters.isEmpty()) return
            if (!klass.isRpcServicePsi()) return
            if (!klass.isConfirmedRpcService()) return

            holder.registerProblem(
                klass.typeParameterList ?: return,
                "Type parameters are not allowed on @RpcService interfaces",
                ProblemHighlightType.GENERIC_ERROR,
            )
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            if (function.typeParameters.isEmpty()) return
            val service = function.containingRpcServiceInterfacePsi() ?: return
            if (!service.isConfirmedRpcService()) return

            holder.registerProblem(
                function.typeParameterList ?: return,
                "Type parameters are not allowed on @RpcService functions",
                ProblemHighlightType.GENERIC_ERROR,
            )
        }
    }

    private fun KtClass.isConfirmedRpcService(): Boolean = analyze(this) {
        hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID)
    }
}
