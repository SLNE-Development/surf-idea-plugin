package dev.slne.surf.idea.surfideaplugin.redis.services.navigation

import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants

enum class RedisHandlerKind(
    val annotationFqn: String,
    val annotationSimpleName: String,
    val targetBaseClassFqn: String,
) {
    EVENT(
        annotationFqn = SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION,
        annotationSimpleName = SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_SIMPLE,
        targetBaseClassFqn = SurfRedisClassNames.REDIS_EVENT_CLASS,
    ),

    REQUEST(
        annotationFqn = SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION,
        annotationSimpleName = SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_SIMPLE,
        targetBaseClassFqn = SurfRedisClassNames.REDIS_REQUEST_CLASS,
    );

    companion object {
        fun fromCallName(name: String): RedisHandlerKind? = when (name) {
            SurfRedisConstants.PUBLISH_EVENT_METHOD_NAME,
            SurfRedisConstants.PUBLISH_METHOD_NAME -> EVENT

            SurfRedisConstants.SEND_REQUEST_METHOD_NAME -> REQUEST

            else -> null
        }
    }
}