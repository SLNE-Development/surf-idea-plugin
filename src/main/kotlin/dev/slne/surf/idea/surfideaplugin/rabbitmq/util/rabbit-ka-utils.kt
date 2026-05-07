@file:OptIn(KaContextParameterApi::class)

package dev.slne.surf.idea.surfideaplugin.rabbitmq.util

import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

context(_: KaSession)
fun KtNamedFunction.isRabbitHandler() = hasAnnotation(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_ID)

context(_: KaSession)
fun KtParameter.isRabbitRequestPacket() =
    symbol.returnType.isSubtypeOf(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID)