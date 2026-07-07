package dev.slne.surf.idea.surfideaplugin.rabbitmq.util

import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

fun KtAnnotated.isRabbitHandlerPsi() = hasAnnotationPsi(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_FQN)

fun KtAnnotated.isRpcServicePsi() = hasAnnotationPsi(SurfRabbitClassNames.RPC_SERVICE_ANNOTATION_FQN)

/**
 * The `@RpcService` interface this declaration is directly declared in, or `null`
 * if the declaration is not a direct member of one. PSI-heuristic only.
 */
fun KtDeclaration.containingRpcServiceInterfacePsi(): KtClass? {
    val containing = containingClassOrObject as? KtClass ?: return null
    if (!containing.isInterface()) return null
    return containing.takeIf { it.isRpcServicePsi() }
}