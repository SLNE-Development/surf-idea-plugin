package dev.slne.surf.idea.surfideaplugin.rabbitmq.actions

/**
 * Generates the Kotlin class body for a Rabbit Packet based on user input.
 */
object RabbitPacketCodeGenerator {

    fun generateRequestBody(
        name: String,
        dataClass: Boolean,
        responseSimpleName: String
    ): String {
        return if (!dataClass) {
            "class $name : RabbitRequestPacket<$responseSimpleName>()"
        } else {
            "data class $name() : RabbitRequestPacket<$responseSimpleName>()"
        }
    }

    fun generateResponseBody(
        name: String,
        dataClass: Boolean
    ): String {
        return if (!dataClass) {
            "class $name : RabbitResponsePacket()"
        } else {
            "data class $name() : RabbitResponsePacket()"
        }
    }
}