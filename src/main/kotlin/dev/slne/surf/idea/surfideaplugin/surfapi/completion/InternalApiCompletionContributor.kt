package dev.slne.surf.idea.surfideaplugin.surfapi.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import dev.slne.surf.idea.surfideaplugin.surfapi.util.InternalApiUtils
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtDeclaration

class InternalApiCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            InternalApiCompletionProvider
        )
        extend(
            CompletionType.SMART,
            PlatformPatterns.psiElement(),
            InternalApiCompletionProvider
        )
    }

    private object InternalApiCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.position
            val module = position.module ?: return

            if (!SurfLibraryDetector.hasSurfApiCore(module)) return
            if (!SurfLibraryDetector.isClassInModuleClasspath(
                    module,
                    SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION
                )
            ) return

            result.runRemainingContributors(parameters) { completionResult ->
                val lookupElement = completionResult.lookupElement
                if (shouldFilterOut(lookupElement, position)) {
                    return@runRemainingContributors
                }
                result.passResult(completionResult)
            }
        }

        private fun shouldFilterOut(lookupElement: LookupElement, position: PsiElement): Boolean {
            val psiElement = lookupElement.psiElement ?: return false
            val ktDeclaration = psiElement as? KtDeclaration ?: return false

            val virtualFile = ktDeclaration.containingFile?.virtualFile ?: return false
            val project = position.project
            val fileIndex = ProjectFileIndex.getInstance(project)
            if (fileIndex.isInSourceContent(virtualFile)) return false

            return try {
                analyze(ktDeclaration) {
                    val symbol = ktDeclaration.symbol
                    InternalApiUtils.isHiddenInternalApi(symbol, position)
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}