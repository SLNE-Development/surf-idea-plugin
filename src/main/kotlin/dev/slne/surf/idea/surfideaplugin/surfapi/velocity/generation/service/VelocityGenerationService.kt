package dev.slne.surf.idea.surfideaplugin.surfapi.velocity.generation.service

import com.intellij.ide.util.ClassFilter
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringBundle
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.common.library.hasLibrary
import dev.slne.surf.idea.surfideaplugin.common.library.hasModuleLibrary
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.surfapi.velocity.VelocityClassNames
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.core.insertMembersAfterAndReformat
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

@Service(Service.Level.PROJECT)
class VelocityGenerationService(
    private val project: Project
) {
    companion object {
        fun getInstance(project: Project): VelocityGenerationService = project.service()
    }

    suspend fun prepareGenerationTarget(
        editor: Editor,
        psiFile: PsiFile,
    ): VelocityGenerationTarget? = readAction(fun(): VelocityGenerationTarget? {
        if (psiFile !is KtFile) return null
        if (DumbService.isDumb(project)) return null

        val module = psiFile.module ?: return null

        if (!module.hasLibrary(SurfLibraryMarker.SURF_API_VELOCITY)) {
            return null
        }

        if (!SurfLibraryDetector.hasClass(module, VelocityClassNames.SUBSCRIBE_ANNOTATION)) {
            return null
        }

        val targetClass = findTargetClass(editor, psiFile) ?: return null

        return VelocityGenerationTarget(
            module = module,
            targetClassPointer = SmartPointerManager
                .createPointer(targetClass),
            existingFunctionNames = existingFunctionNames(targetClass),
        )
    })

    fun isAvailable(
        editor: Editor,
        psiFile: PsiFile,
    ): Boolean {
        if (psiFile !is KtFile) return false
        if (DumbService.isDumb(project)) return false

        if (!psiFile.hasModuleLibrary(SurfLibraryMarker.SURF_API_VELOCITY)) {
            return false
        }

        return findTargetClass(editor, psiFile) != null
    }

    fun chooseEventClass(
        module: Module,
    ): VelocityEventClassSelection? {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(
            module,
            false,
        )

        val chooser = TreeClassChooserFactory.getInstance(project)
            .createWithInnerClassesScopeChooser(
                RefactoringBundle.message("choose.destination.class"),
                scope,
                VelocityEventClassFilter,
                null,
            )

        chooser.showDialog()

        val selected = chooser.selected ?: return null

        val name = selected.name ?: return null
        val qualifiedName = selected.qualifiedName ?: return null

        return VelocityEventClassSelection(
            shortName = name,
            qualifiedName = qualifiedName,
        )
    }

    fun createDefaultHandlerName(
        eventClassName: String,
        existingFunctionNames: Set<String>,
    ): String {
        val eventName = eventClassName
            .removeSuffix("Event")
            .ifBlank { eventClassName }

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

    suspend fun generateEventListener(
        editor: Editor,
        targetClassPointer: SmartPsiElementPointer<KtClassOrObject>,
        config: VelocityEventListenerGenerationConfig,
    ) {
        writeCommandAction(project, "Generate Velocity Event Listener") {
            val targetClass = targetClassPointer.element ?: return@writeCommandAction
            val factory = KtPsiFactory(project)

            val functionText = FunSpec.builder(config.handlerName)
                .addParameter("event", ClassName.bestGuess(config.eventClassFqName))
                .applyIf(config.suspendHandler) {
                    addModifiers(KModifier.SUSPEND)
                }
                .addAnnotation(
                    AnnotationSpec.builder(VelocityClassNames.SUBSCRIBE_ANNOTATION_CLASS_NAME)
                        .applyIf(config.priority != 0) {
                            addMember("priority = %L", config.priority)
                        }
                        .build()
                )
                .build()
                .toString()


            val prototype = factory.createFunction(functionText)

            val anchor = targetClass.declarations.lastIsInstanceOrNull<KtNamedFunction>()
                ?: targetClass.declarations.lastOrNull()

            val insertedFunction = insertMembersAfterAndReformat(
                editor,
                targetClass,
                prototype,
                anchor,
            )

            moveCaretIntoFunctionBody(
                editor = editor,
                function = insertedFunction,
            )
        }
    }

    private fun findTargetClass(
        editor: Editor,
        file: PsiFile,
    ): KtClassOrObject? {
        val offset = editor.caretModel.offset

        val caretElement = file.findElementAt(offset)
            ?: file.findElementAt((offset - 1).coerceAtLeast(0))
            ?: return null

        return caretElement.getParentOfType(strict = false)
    }

    private fun existingFunctionNames(
        targetClass: KtClassOrObject,
    ): Set<String> {
        return targetClass.declarations
            .asSequence()
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.name }
    }

    private fun moveCaretIntoFunctionBody(
        editor: Editor,
        function: KtNamedFunction,
    ) {
        val body = function.bodyBlockExpression ?: return
        val lBrace = body.lBrace ?: return

        editor.caretModel.moveToOffset(lBrace.textRange.endOffset)
    }

    private object VelocityEventClassFilter : ClassFilter {
        override fun isAccepted(
            aClass: PsiClass,
        ): Boolean {
            return !aClass.isInterface &&
                    !aClass.isAnnotationType &&
                    !aClass.hasModifierProperty(PsiModifier.ABSTRACT)
        }
    }

    data class VelocityGenerationTarget(
        val module: Module,
        val targetClassPointer: SmartPsiElementPointer<KtClassOrObject>,
        val existingFunctionNames: Set<String>,
    )

    data class VelocityEventClassSelection(
        val shortName: String,
        val qualifiedName: String,
    )

    data class VelocityEventListenerGenerationConfig(
        val handlerName: String,
        val eventClassFqName: String,
        val priority: Int,
        val suspendHandler: Boolean,
    )

}

fun Project.velocityGenerationService(): VelocityGenerationService = VelocityGenerationService.getInstance(this)