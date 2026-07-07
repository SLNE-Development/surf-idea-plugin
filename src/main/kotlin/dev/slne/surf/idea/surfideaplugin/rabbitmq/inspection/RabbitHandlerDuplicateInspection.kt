package dev.slne.surf.idea.surfideaplugin.rabbitmq.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfKotlinInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRabbitHandlerPsi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classOrObjectVisitor

/**
 * Only one `@RabbitHandler` may be registered per request packet type; a second handler
 * for the same packet in the registered instance fails with a duplicate-handler exception.
 */
class RabbitHandlerDuplicateInspection : SurfKotlinInspection(SurfLibraryMarker.SURF_RABBITMQ_SERVER_API) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classOrObjectVisitor(fun(classOrObject: KtClassOrObject) {
        val handlers = classOrObject.declarations
            .filterIsInstance<KtNamedFunction>()
            .filter { it.isRabbitHandlerPsi() && it.valueParameters.isNotEmpty() }

        if (handlers.size < 2) return

        val handlersByPacket = analyze(classOrObject) {
            handlers.groupBy { handler ->
                val parameterType = handler.valueParameters.first().symbol.returnType as? KaClassType
                parameterType?.classId
            }
        }

        for ((packetClassId: ClassId?, packetHandlers) in handlersByPacket) {
            if (packetClassId == null || packetHandlers.size < 2) continue

            for (handler in packetHandlers.drop(1)) {
                holder.registerProblem(
                    handler.nameIdentifier ?: handler,
                    "Duplicate @RabbitHandler for packet type '${packetClassId.shortClassName.asString()}' — " +
                            "only one handler per packet type can be registered",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    })
}
