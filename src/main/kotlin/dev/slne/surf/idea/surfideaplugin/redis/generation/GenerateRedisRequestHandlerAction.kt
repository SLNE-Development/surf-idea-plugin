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
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisConstants
import dev.slne.surf.idea.surfideaplugin.redis.generation.ui.RedisHandlerNameSelectionDialog
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

class GenerateRedisRequestHandlerAction : CodeInsightActionHandler {

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
        val defaultHandlerName = "handle$shortChosenClassName"

        val dialog =
            RedisHandlerNameSelectionDialog(editor, "Redis Request Class", shortChosenClassName, defaultHandlerName)
        if (!dialog.showAndGet()) return

        currentThreadCoroutineScope().launch {
            generateEventHandler(
                project = project,
                editor = editor,
                targetClass = targetClass,
                handlerName = dialog.chosenName,
                requestClassName = fqChosenClassName
            )
        }
    }

    private suspend fun generateEventHandler(
        project: Project,
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        requestClassName: String
    ) {
        val annotationFqn = SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION
        val requestContext = SurfRedisClassNames.REQUEST_CONTEXT_CLASS

        val functionText = """
            @$annotationFqn
            fun $handlerName(${SurfRedisConstants.REDIS_REQUEST_HANDLER_PARAMETER_NAME}: $requestContext<$requestClassName>) {
            }
        """.trimIndent()

        writeCommandAction(project, "Generate Redis Request Handler") {
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
            return InheritanceUtil.isInheritor(aClass, SurfRedisClassNames.REDIS_REQUEST_CLASS)
        }
    }
}