package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.service

import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import dev.slne.surf.idea.surfideaplugin.common.service.inheritanceService
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.PaperClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.util.PaperEventListenerPriorities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

@Service(Service.Level.PROJECT)
class PaperGenerationService(
    private val project: Project,
    private val scope: CoroutineScope
) {

    companion object {
        private val LISTENER_INTERFACE = ClassId.topLevel(FqName(PaperClassNames.LISTENER_CLASS))
        fun getInstance(project: Project): PaperGenerationService = project.service()
    }

    suspend fun generateEventListener(
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        eventClassName: String,
        priority: PaperEventListenerPriorities?,
        ignoreCancelled: Boolean
    ) {
        val annotationParams = buildList {
            if (priority != PaperEventListenerPriorities.NORMAL && priority != null) {
                add("priority = ${PaperClassNames.EVENT_PRIORITY_CLASS}.$priority")
            }
            if (ignoreCancelled) {
                add("ignoreCancelled = true")
            }
        }

        val annotationText = if (annotationParams.isEmpty()) {
            "@${PaperClassNames.EVENT_HANDLER_ANNOTATION}"
        } else {
            "@${PaperClassNames.EVENT_HANDLER_ANNOTATION}(${annotationParams.joinToString(", ")})"
        }

        val functionText = """
            $annotationText
            fun $handlerName(event: $eventClassName) {
            }
        """.trimIndent()

        writeCommandAction(project, "Generate Paper Event Listener") {
            val factory = KtPsiFactory(project)
            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            insertMembersAfterAndReformat(editor, targetClass, listOf(prototype), anchor)
        }

        scope.launch {
            project.inheritanceService().implementInterfaceIfMissing(targetClass, LISTENER_INTERFACE)
        }
    }
}

fun Project.paperGenerationService(): PaperGenerationService = PaperGenerationService.getInstance(this)