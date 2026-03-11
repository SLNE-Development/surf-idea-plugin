package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service

import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.VelocityClassNames
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

@Service(Service.Level.PROJECT)
class VelocityGenerationService(
    private val project: Project
) {
    companion object {
        fun getInstance(project: Project): VelocityGenerationService = project.service()
    }

    suspend fun generateEventListener(
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        eventClassName: String,
        priority: Int,
        suspendHandler: Boolean
    ) {
        val annotationParams = buildList {
            if (priority != 0) {
                add("priority = ${priority.toShort()}")
            }
        }

        val annotationText = if (annotationParams.isEmpty()) {
            "@${VelocityClassNames.SUBSCRIBE_ANNOTATION}"
        } else {
            "@${VelocityClassNames.SUBSCRIBE_ANNOTATION}(${annotationParams.joinToString(", ")})"
        }

        val modifierText = if (suspendHandler) KtTokens.SUSPEND_KEYWORD.value + " " else ""
        val functionText = """
            $annotationText
            ${modifierText}fun $handlerName(event: $eventClassName) {
            }
        """.trimIndent()

        writeCommandAction(project, "Generate Velocity Event Listener") {
            val factory = KtPsiFactory(project)
            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            insertMembersAfterAndReformat(editor, targetClass, listOf(prototype), anchor)
        }
    }
}

fun Project.velocityGenerationService(): VelocityGenerationService = VelocityGenerationService.getInstance(this)