package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection.rpc

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfKotlinInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRpcServicePsi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classVisitor

/**
 * RPC callables are addressed by function name, so `@RpcService` interfaces cannot
 * declare two endpoints with the same name (method overloading is unsupported).
 */
class RpcServiceOverloadInspection : SurfKotlinInspection(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classVisitor(fun(klass: KtClass) {
        if (!klass.isInterface()) return
        if (!klass.isRpcServicePsi()) return

        val endpointsByName = klass.declarations
            .filterIsInstance<KtNamedFunction>()
            .filter { it.name != null && it.name !in RpcServiceSuspendFunctionInspection.IGNORED_OBJECT_METHODS }
            .groupBy { it.name }
            .filterValues { it.size > 1 }

        if (endpointsByName.isEmpty()) return

        val isRpcService = analyze(klass) {
            klass.hasAnnotation(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_ID)
        }
        if (!isRpcService) return

        for ((name, overloads) in endpointsByName) {
            for (function in overloads.drop(1)) {
                holder.registerProblem(
                    function.nameIdentifier ?: function,
                    "RPC method overloading is not supported: a function named '$name' is already defined",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    })
}
