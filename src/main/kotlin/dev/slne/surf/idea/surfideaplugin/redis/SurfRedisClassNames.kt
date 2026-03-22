package dev.slne.surf.idea.surfideaplugin.redis

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfRedisClassNames {
    const val ON_REDIS_EVENT_ANNOTATION = "dev.slne.surf.redis.event.OnRedisEvent"
    const val ON_REDIS_EVENT_ANNOTATION_SIMPLE = "OnRedisEvent"
    const val HANDLE_REDIS_REQUEST_ANNOTATION = "dev.slne.surf.redis.request.HandleRedisRequest"
    const val HANDLE_REDIS_REQUEST_ANNOTATION_SIMPLE = "HandleRedisRequest"

    const val REDIS_EVENT_CLASS = "dev.slne.surf.redis.event.RedisEvent"
    const val REDIS_REQUEST_CLASS = "dev.slne.surf.redis.request.RedisRequest"
    const val REDIS_RESPONSE_CLASS = "dev.slne.surf.redis.request.RedisResponse"

    const val REQUEST_CONTEXT_CLASS = "dev.slne.surf.redis.request.RequestContext"

    // Fqs
    val ON_REDIS_EVENT_ANNOTATION_FQN = FqName(ON_REDIS_EVENT_ANNOTATION)
    val HANDLE_REDIS_REQUEST_ANNOTATION_FQN = FqName(HANDLE_REDIS_REQUEST_ANNOTATION)
    val REDIS_EVENT_CLASS_FQN = FqName(REDIS_EVENT_CLASS)
    val REDIS_REQUEST_CLASS_FQN = FqName(REDIS_REQUEST_CLASS)
    val REDIS_RESPONSE_CLASS_FQN = FqName(REDIS_RESPONSE_CLASS)
    val REQUEST_CONTEXT_CLASS_FQN = FqName(REQUEST_CONTEXT_CLASS)

    // Class ids
    val ON_REDIS_EVENT_ANNOTATION_ID = ClassId.topLevel(ON_REDIS_EVENT_ANNOTATION_FQN)
    val HANDLE_REDIS_REQUEST_ANNOTATION_ID = ClassId.topLevel(HANDLE_REDIS_REQUEST_ANNOTATION_FQN)
    val REDIS_EVENT_CLASS_ID = ClassId.topLevel(REDIS_EVENT_CLASS_FQN)
    val REDIS_REQUEST_CLASS_ID = ClassId.topLevel(REDIS_REQUEST_CLASS_FQN)
    val REDIS_RESPONSE_CLASS_ID = ClassId.topLevel(REDIS_RESPONSE_CLASS_FQN)
    val REQUEST_CONTEXT_CLASS_ID = ClassId.topLevel(REQUEST_CONTEXT_CLASS_FQN)
}