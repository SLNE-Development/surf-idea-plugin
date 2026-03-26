package dev.slne.surf.idea.surfideaplugin.rabbitmq

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfRabbitClassNames {

    const val RABBIT_REQUEST_PACKET_CLASS = "dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket"
    const val RABBIT_HANDLER_ANNOTATION = "dev.slne.surf.rabbitmq.api.handler.RabbitHandler"

    val RABBIT_REQUEST_PACKET_CLASS_FQN = FqName(RABBIT_REQUEST_PACKET_CLASS)
    val RABBIT_HANDLER_ANNOTATION_FQN = FqName(RABBIT_HANDLER_ANNOTATION)

    val RABBIT_REQUEST_PACKET_CLASS_ID = ClassId.topLevel(RABBIT_REQUEST_PACKET_CLASS_FQN)
    val RABBIT_HANDLER_ANNOTATION_ID = ClassId.topLevel(RABBIT_HANDLER_ANNOTATION_FQN)
}