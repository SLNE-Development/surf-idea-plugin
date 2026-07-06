package dev.slne.surf.idea.surfideaplugin.common.library

enum class SurfLibraryMarker(val fqn: String) {
    SURF_API_CORE("dev.slne.surf.api.core.SurfApiCore"),
    SURF_API_PAPER("dev.slne.surf.api.paper.SurfApiPaper"),
    SURF_API_VELOCITY("dev.slne.surf.api.velocity.SurfApiVelocity"),

    SURF_REDIS_API("dev.slne.surf.redis.RedisApi"),

    SURF_DATABASE_API("dev.slne.surf.database.DatabaseApi"),

    SURF_RABBITMQ_COMMON_API("dev.slne.surf.rabbitmq.api.RabbitMQApi"),
    SURF_RABBITMQ_SERVER_API("dev.slne.surf.rabbitmq.api.ServerRabbitMQApi"),
    SURF_RABBITMQ_CLIENT_API("dev.slne.surf.rabbitmq.api.ClientRabbitMQApi"),
}