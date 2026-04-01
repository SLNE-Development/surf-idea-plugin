package dev.slne.surf.idea.surfideaplugin.rabbitmq.util

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiClass
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.allSupertypes
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.base.psi.classIdIfNonLocal
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass

/**
 * Resolves the response type of a RabbitRequestPacket subclass.
 *
 * Given a PsiClass (must be a Kotlin class via KtLightClass), finds the first
 * type argument of the nearest supertype that looks like RabbitRequestPacket<R>.
 *
 * Returns the class id,
 * or null if this class is not a concrete RabbitRequestPacket subtype.
 */
suspend fun resolveRabbitResponseTypeName(psiClass: PsiClass): ClassId? {
    val ktLightClass = psiClass as? KtLightClass ?: return null
    val ktClass = ktLightClass.kotlinOrigin as? KtClass ?: return null

    return readAction {
        analyze(ktClass) {
            val classSymbol = ktClass.symbol as? KaNamedClassSymbol ?: return@analyze null
            val requestPacketSuperType = classSymbol.defaultType
                .allSupertypes
                .filterIsInstance<KaClassType>()
                .firstOrNull { superType ->
                    superType.expandedSymbol?.classId == SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID
                } ?: return@analyze null

            val responseTypeArg = requestPacketSuperType.typeArguments
                .firstOrNull() ?: return@analyze null

            val responseType = responseTypeArg.type as? KtClass ?: return@analyze null
            responseType.classIdIfNonLocal
        }
    }
}

object RabbitRespondTypeResolver {

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    fun resolveResponseTypeName(expressionType: KaType): ClassId? {
        val requestPacketSuperType = expressionType.allSupertypes
            .filterIsInstance<KaClassType>()
            .firstOrNull {
                val classId = it.classId
                classId == SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID
            } ?: return null

        val responseType = requestPacketSuperType.typeArguments
            .firstOrNull()
            ?.type as? KaClassType
            ?: return null

        return responseType.classId
    }
}