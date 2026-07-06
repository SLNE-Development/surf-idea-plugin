package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import org.jetbrains.kotlin.name.Name

enum class RedisHandlerGenerationKind {
    EVENT {
        override val dialogTitle: String = "Generate Redis Event Handler"
        override val chooserTitle: String = "Choose Redis Event Class"
        override val targetDisplayName: String = "Redis Event Class"
        override val commandName: String = "Generate Redis Event Handler"
        override val baseClassFqn: String = SurfRedisClassNames.REDIS_EVENT_CLASS
        override val annotationClassName: ClassName = SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION_NAME
        override val parameterName: String = SurfRedisConstants.REDIS_EVENT_HANDLER_PARAMETER_NAME

        override fun createDefaultHandlerName(
            selectedClassName: String,
            existingFunctionNames: Set<String>,
        ): String {
            val eventName = selectedClassName
                .removeSuffix("Event")
                .ifBlank { selectedClassName }

            return uniqueFunctionName(
                baseName = "on$eventName",
                existingFunctionNames = existingFunctionNames,
            )
        }

        override fun createParameterTypeName(
            selectedClass: ClassName,
        ): TypeName {
            return selectedClass
        }
    },
    REQUEST {
        override val dialogTitle: String = "Generate Redis Request Handler"
        override val chooserTitle: String = "Choose Redis Request Class"
        override val targetDisplayName: String = "Redis Request Class"
        override val commandName: String = "Generate Redis Request Handler"
        override val baseClassFqn: String = SurfRedisClassNames.REDIS_REQUEST_CLASS
        override val annotationClassName: ClassName = SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION_NAME
        override val parameterName: String = SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME

        override fun createDefaultHandlerName(
            selectedClassName: String,
            existingFunctionNames: Set<String>,
        ): String {
            val requestName = selectedClassName
                .removeSuffix("Request")
                .ifBlank { selectedClassName }

            return uniqueFunctionName(
                baseName = "handle$requestName",
                existingFunctionNames = existingFunctionNames,
            )
        }

        override fun createParameterTypeName(
            selectedClass: ClassName,
        ): TypeName {
            return SurfRedisClassNames.REQUEST_CONTEXT_CLASS_NAME.parameterizedBy(selectedClass)
        }
    };

    abstract val dialogTitle: String
    abstract val chooserTitle: String
    abstract val targetDisplayName: String
    abstract val commandName: String
    abstract val baseClassFqn: String
    abstract val annotationClassName: ClassName
    abstract val parameterName: String

    abstract fun createDefaultHandlerName(
        selectedClassName: String,
        existingFunctionNames: Set<String>,
    ): String

    abstract fun createParameterTypeName(
        selectedClass: ClassName,
    ): TypeName

    protected fun uniqueFunctionName(
        baseName: String,
        existingFunctionNames: Set<String>,
    ): String {
        val normalizedBaseName = baseName
            .takeIf(Name::isValidIdentifier)
            ?: "handleRedis"

        if (normalizedBaseName !in existingFunctionNames) {
            return normalizedBaseName
        }

        var index = 2
        while (true) {
            val candidate = "$normalizedBaseName$index"

            if (candidate !in existingFunctionNames) {
                return candidate
            }

            index++
        }
    }
}