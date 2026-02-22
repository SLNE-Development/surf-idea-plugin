package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.refactoring.RefactoringBundle
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import dev.slne.surf.idea.surfideaplugin.redis.generation.ui.RedisEventHandlerGenerationDialog
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

class GenerateRedisEventHandlerAction : CodeInsightActionHandler {

    override fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {
        if (!SurfLibraryDetector.hasSurfRedis(psiFile)) return
        val module = psiFile.module ?: return
        val caretElement = psiFile.findElementAt(editor.caretModel.offset) ?: return
        val targetClass = caretElement.getParentOfType<KtClassOrObject>(strict = false) ?: return

        if (!EditorModificationUtil.requestWriting(editor)) {
            return
        }

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                RefactoringBundle.message("choose.destination.class"),
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false),
                RedisEventClassFilter,
                null
            )

        chooser.showDialog()
        val chosenClass = chooser.selected ?: return
        val shortChosenClassName = chosenClass.nameIdentifier?.text ?: return
        val fqChosenClassName = chosenClass.qualifiedName ?: return
        val defaultHandlerName = "on" + shortChosenClassName.removeSuffix("Event")

        val dialog = RedisEventHandlerGenerationDialog(editor, shortChosenClassName, defaultHandlerName)
        if (!dialog.showAndGet()) return

        currentThreadCoroutineScope().launch {
            generateEventHandler(
                project = project,
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.chosenName,
                eventClassName = fqChosenClassName
            )
        }
    }

    private suspend fun generateEventHandler(
        project: Project,
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        eventClassName: String
    ) {
        val annotationFqn = SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION

        val paramName = "event"

        val functionText = """
            @$annotationFqn
            fun $handlerName($paramName: $eventClassName) {
            }
        """.trimIndent()

        writeCommandAction(project, "Generate Redis Event Handler") {
            val factory = KtPsiFactory(project)
            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            val inserted = insertMembersAfterAndReformat(editor, targetClass, listOf(prototype), anchor)
            inserted.firstOrNull()?.let {
                ShortenReferencesFacility.getInstance().shorten(it)
            }
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    private object RedisEventClassFilter : ClassFilter {
        override fun isAccepted(aClass: PsiClass): Boolean {
            if (!aClass.isConcreteClass()) return false
            return InheritanceUtil.isInheritor(aClass, SurfRedisClassNames.REDIS_EVENT_CLASS)
        }
    }
}