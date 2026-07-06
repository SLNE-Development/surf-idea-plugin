package dev.slne.surf.idea.surfideaplugin.rabbitmq.actions

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import dev.slne.surf.idea.surfideaplugin.util.className
import dev.slne.surf.idea.surfideaplugin.util.serializableAnnotationSpec


object RabbitPacketCodeGenerator {

    const val CARET_MARKER = "/*__SURF_CARET__*/"

    data class GeneratedKotlinFile(
        val fileName: String,
        val source: String,
        val caretMarker: String? = null
    )

    fun generateRequestPacketFile(
        packageName: String,
        requestName: String,
        responseSimpleName: String,
        responseFqn: String?
    ): GeneratedKotlinFile {
        val responseType = className(
            packageName = packageName,
            simpleName = responseSimpleName,
            fqName = responseFqn
        )

        val requestType = TypeSpec.classBuilder(requestName)
            .addAnnotation(serializableAnnotationSpec())
            .superclass(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_NAME.parameterizedBy(responseType))
            .build()

        return GeneratedKotlinFile(
            fileName = "$requestName.kt",
            source = fileSource(packageName, requestName, requestType)
        )
    }

    fun generateResponsePacketFile(
        packageName: String,
        responseName: String
    ): GeneratedKotlinFile {
        val responseType = TypeSpec.classBuilder(responseName)
            .addAnnotation(serializableAnnotationSpec())
            .superclass(SurfRabbitClassNames.RABBIT_RESPONSE_PACKET_CLASS_NAME)
            .build()

        return GeneratedKotlinFile(
            fileName = "$responseName.kt",
            source = fileSource(packageName, responseName, responseType)
        )
    }

    fun generateHandlerFile(
        packageName: String,
        handlerClassName: String,
        handlerMethodName: String,
        requestFqn: String,
        destructuringParameterNames: String?
    ): GeneratedKotlinFile {
        val requestType = ClassName.bestGuess(requestFqn)

        val handlerMethod = FunSpec.builder(handlerMethodName)
            .addAnnotation(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_NAME)
            .addModifiers(KModifier.SUSPEND)
            .addParameter(SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME, requestType)
            .addCode(handlerBody(destructuringParameterNames))
            .build()

        val handlerObject = TypeSpec.objectBuilder(handlerClassName)
            .addFunction(handlerMethod)
            .build()

        return GeneratedKotlinFile(
            fileName = "$handlerClassName.kt",
            source = fileSource(packageName, handlerClassName, handlerObject),
            caretMarker = CARET_MARKER
        )
    }

    private fun handlerBody(destructuringParameterNames: String?): CodeBlock {
        return CodeBlock.builder()
            .apply {
                if (!destructuringParameterNames.isNullOrBlank()) {
                    addStatement(
                        "val (%L) = %L",
                        destructuringParameterNames,
                        SurfRabbitConstants.RABBIT_HANDLER_PARAMETER_NAME
                    )
                }

                add("%L\n", CARET_MARKER)
            }
            .build()
    }


    private fun fileSource(
        packageName: String,
        fileName: String,
        type: TypeSpec
    ): String = FileSpec.builder(packageName, fileName)
        .addType(type)
        .build()
        .toString()


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