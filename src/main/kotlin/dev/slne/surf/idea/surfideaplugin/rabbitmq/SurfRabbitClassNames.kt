package dev.slne.surf.idea.surfideaplugin.rabbitmq

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfRabbitClassNames {

    const val RABBIT_REQUEST_PACKET_CLASS = "dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket"
    const val RABBIT_RESPONSE_PACKET_CLASS = "dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket"
    const val RABBIT_PACKET_CLASS = "dev.slne.surf.rabbitmq.api.packet.RabbitPacket"
    const val RABBIT_HANDLER_ANNOTATION = "dev.slne.surf.rabbitmq.api.handler.RabbitHandler"
    const val CLIENT_RABBIT_MQ_API_CLASS = "dev.slne.surf.rabbitmq.api.ClientRabbitMQApi"
    const val RPC_SERVICE_ANNOTATION = "dev.slne.surf.rabbitmq.api.rpc.RpcService"

    val RABBIT_REQUEST_PACKET_CLASS_FQN = FqName(RABBIT_REQUEST_PACKET_CLASS)
    val RABBIT_RESPONSE_PACKET_CLASS_FQN = FqName(RABBIT_RESPONSE_PACKET_CLASS)
    val RABBIT_PACKET_CLASS_FQN = FqName(RABBIT_PACKET_CLASS)
    val RABBIT_HANDLER_ANNOTATION_FQN = FqName(RABBIT_HANDLER_ANNOTATION)
    val CLIENT_RABBIT_MQ_API_CLASS_FQN = FqName(CLIENT_RABBIT_MQ_API_CLASS)
    val RPC_SERVICE_ANNOTATION_FQN = FqName(RPC_SERVICE_ANNOTATION)

    val RABBIT_REQUEST_PACKET_CLASS_ID = ClassId.topLevel(RABBIT_REQUEST_PACKET_CLASS_FQN)
    val RABBIT_RESPONSE_PACKET_CLASS_ID = ClassId.topLevel(RABBIT_RESPONSE_PACKET_CLASS_FQN)
    val RABBIT_PACKET_CLASS_ID = ClassId.topLevel(RABBIT_PACKET_CLASS_FQN)
    val RABBIT_HANDLER_ANNOTATION_ID = ClassId.topLevel(RABBIT_HANDLER_ANNOTATION_FQN)
    val CLIENT_RABBIT_MQ_API_CLASS_ID = ClassId.topLevel(CLIENT_RABBIT_MQ_API_CLASS_FQN)
    val RPC_SERVICE_ANNOTATION_ID = ClassId.topLevel(RPC_SERVICE_ANNOTATION_FQN)

    val RABBIT_REQUEST_PACKET_CLASS_NAME = ClassName.bestGuess(RABBIT_REQUEST_PACKET_CLASS)
    val RABBIT_RESPONSE_PACKET_CLASS_NAME = ClassName.bestGuess(RABBIT_RESPONSE_PACKET_CLASS)
    val RABBIT_HANDLER_ANNOTATION_NAME = ClassName.bestGuess(RABBIT_HANDLER_ANNOTATION)
}