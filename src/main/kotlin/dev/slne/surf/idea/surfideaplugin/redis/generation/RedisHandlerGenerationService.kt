package dev.slne.surf.idea.surfideaplugin.redis.generation

import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import dev.slne.surf.idea.surfideaplugin.common.facet.hasLibrary
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.isConcreteClass
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

@Service(Service.Level.PROJECT)
class RedisHandlerGenerationService(
    private val project: Project,
) {
    fun isAvailable(
        editor: Editor,
        file: PsiFile,
    ): Boolean {
        if (file !is KtFile) return false
        if (DumbService.isDumb(project)) return false

        val module = file.module ?: return false

        if (!module.hasLibrary(SurfLibraryMarker.SURF_REDIS_API)) {
            return false
        }

        return findTargetClass(editor, file) != null
    }

    fun findTargetClass(
        editor: Editor,
        file: PsiFile,
    ): KtClassOrObject? {
        val caretElement = file.findElementAt(editor.caretModel.offset)
            ?: file.findElementAt((editor.caretModel.offset - 1).coerceAtLeast(0))
            ?: return null

        return caretElement.getParentOfType(strict = false)
    }

    fun chooseRedisClass(
        module: Module,
        kind: RedisHandlerGenerationKind,
    ): PsiClass? {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(
            module,
            false,
        )

        val baseClass = JavaPsiFacade.getInstance(project)
            .findClass(kind.baseClassFqn, scope)
            ?: return null

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                kind.chooserTitle,
                scope,
                RedisClassFilter(baseClass),
                null,
            )

        chooser.showDialog()

        return chooser.selected
    }

    fun generateHandler(
        editor: Editor,
        targetClass: KtClassOrObject,
        kind: RedisHandlerGenerationKind,
        handlerName: String,
        selectedClass: ClassName,
    ) {
        if (!EditorModificationUtil.requestWriting(editor)) {
            return
        }

        WriteCommandAction.runWriteCommandAction(
            project,
            kind.commandName,
            null,
            {
                val factory = KtPsiFactory(project)

                val prototype = factory.createFunction(
                    createFunctionText(
                        kind = kind,
                        handlerName = handlerName,
                        selectedClass = selectedClass,
                    ),
                )

                val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                    ?: targetClass.declarations.lastOrNull()

                val insertedDeclarations = insertMembersAfterAndReformat(
                    editor,
                    targetClass,
                    listOf(prototype),
                    anchor,
                )

                val insertedFunction = insertedDeclarations
                    .firstOrNull()
                    ?: return@runWriteCommandAction

                ShortenReferencesFacility
                    .getInstance()
                    .shorten(insertedFunction)

                moveCaretIntoFunctionBody(
                    editor,
                    insertedFunction,
                )
            },
            targetClass.containingFile,
        )
    }

    fun existingFunctionNames(
        targetClass: KtClassOrObject,
    ): Set<String> {
        return targetClass.declarations
            .asSequence()
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.name }
    }

    private fun createFunctionText(
        kind: RedisHandlerGenerationKind,
        handlerName: String,
        selectedClass: ClassName,
    ): String = FunSpec.builder(handlerName)
        .addAnnotation(kind.annotationClassName)
        .addParameter(kind.parameterName, kind.createParameterTypeName(selectedClass))
        .build()
        .toString()


    private fun moveCaretIntoFunctionBody(
        editor: Editor,
        function: KtNamedFunction,
    ) {
        val body = function.bodyBlockExpression ?: return
        val lBrace = body.lBrace ?: return

        editor.caretModel.moveToOffset(lBrace.textRange.endOffset)
    }

    private class RedisClassFilter(
        private val baseClass: PsiClass,
    ) : ClassFilter {

        override fun isAccepted(
            aClass: PsiClass,
        ): Boolean {
            if (!aClass.isConcreteClass()) return false

            return InheritanceUtil.isInheritorOrSelf(
                aClass,
                baseClass,
                true,
            )
        }
    }

    companion object {
        fun getInstance(project: Project): RedisHandlerGenerationService = project.service()
    }
}

fun Project.redisHandlerGenerationService(): RedisHandlerGenerationService =
    RedisHandlerGenerationService.getInstance(this)