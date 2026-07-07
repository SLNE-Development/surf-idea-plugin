package dev.slne.surf.idea.surfideaplugin.surfapi.paper.generation.service

import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.refactoring.RefactoringBundle
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.hasLibrary
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.service.inheritanceService
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.PaperClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.paper.util.PaperEventListenerPriorities
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

@Service(Service.Level.PROJECT)
class PaperGenerationService(
    private val project: Project,
) {

    companion object {
        fun getInstance(project: Project): PaperGenerationService = project.service()
    }

    fun isAvailable(
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        if (file !is KtFile) return false
        if (DumbService.isDumb(project)) return false

        val module = file.module ?: return false
        if (!module.hasLibrary(SurfLibraryMarker.SURF_API_PAPER)) {
            return false
        }

        if (!SurfLibraryDetector.hasClass(module, PaperClassNames.EVENT_CLASS)) {
            return false
        }

        return findTargetClass(editor, file) != null
    }

    fun findTargetClass(
        editor: Editor,
        file: PsiFile,
    ): KtClassOrObject? {
        val offset = editor.caretModel.offset

        val element = file.findElementAt(offset)
            ?: file.findElementAt((offset - 1).coerceAtLeast(0))
            ?: return null

        return element.getParentOfType(strict = false)
    }

    fun chooseEventClass(
        module: Module,
    ): PsiClass? {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(
            module,
            false,
        )

        val eventBaseClass = JavaPsiFacade.getInstance(project)
            .findClass(PaperClassNames.EVENT_CLASS, scope)
            ?: return null

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                RefactoringBundle.message("choose.destination.class"),
                scope,
                PaperEventClassFilter(eventBaseClass),
                null,
            )

        chooser.showDialog()

        return chooser.selected
    }

    fun createDefaultHandlerName(
        eventClassShortName: String,
        existingFunctionNames: Set<String>,
    ): String {
        val eventName = eventClassShortName
            .removeSuffix("Event")
            .ifBlank { eventClassShortName }

        val baseName = "on$eventName"

        if (baseName !in existingFunctionNames) {
            return baseName
        }

        var index = 2
        while (true) {
            val candidate = "$baseName$index"

            if (candidate !in existingFunctionNames) {
                return candidate
            }

            index++
        }
    }

    fun existingFunctionNames(
        targetClass: KtClassOrObject,
    ): Set<String> {
        return targetClass.declarations
            .asSequence()
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.name }
    }


    suspend fun generateEventListener(
        editor: Editor,
        targetClass: KtClassOrObject,
        handlerName: String,
        eventClassName: TypeName,
        priority: PaperEventListenerPriorities?,
        ignoreCancelled: Boolean
    ) {
        val functionText = FunSpec.builder(handlerName)
            .addParameter("event", eventClassName)
            .addAnnotation(
                AnnotationSpec.builder(PaperClassNames.EVENT_HANDLER_ANNOTATION_CLASS_NAME)
                    .addMember(
                        "priority = %T.%L",
                        PaperClassNames.EVENT_PRIORITY_CLASS_NAME,
                        priority ?: PaperEventListenerPriorities.NORMAL
                    )
                    .addMember("ignoreCancelled = %L", ignoreCancelled)
                    .build()
            )
            .toString()

        project.inheritanceService().implementInterfaceIfMissing(targetClass, PaperClassNames.LISTENER_CLASS_ID)

        writeCommandAction(project, "Generate Paper Event Listener") {
            val factory = KtPsiFactory(project)
            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            val insertedFunction = insertMembersAfterAndReformat(editor, targetClass, prototype, anchor)

            moveCaretIntoFunctionBody(
                editor = editor,
                function = insertedFunction,
            )
        }
    }

    private fun moveCaretIntoFunctionBody(
        editor: Editor,
        function: KtNamedFunction,
    ) {
        val body = function.bodyBlockExpression ?: return
        val lBrace = body.lBrace ?: return

        editor.caretModel.moveToOffset(lBrace.textRange.endOffset)
    }

    private class PaperEventClassFilter(
        private val eventBaseClass: PsiClass,
    ) : ClassFilter {

        override fun isAccepted(aClass: PsiClass): Boolean {
            if (!aClass.isConcreteClass()) return false

            if (!InheritanceUtil.isInheritorOrSelf(aClass, eventBaseClass, true)) {
                return false
            }

            return aClass.methods.any { method ->
                method.name == "getHandlerList" &&
                        method.hasModifierProperty(PsiModifier.STATIC) &&
                        method.parameterList.parametersCount == 0
            }
        }

        private fun PsiClass.isConcreteClass(): Boolean {
            return !isInterface &&
                    !isAnnotationType &&
                    !hasModifierProperty(PsiModifier.ABSTRACT)
        }
    }
}

fun Project.paperGenerationService(): PaperGenerationService = PaperGenerationService.getInstance(this)