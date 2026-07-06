package dev.slne.surf.idea.surfideaplugin.surfapi.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import dev.slne.surf.idea.surfideaplugin.common.facet.hasLibrary
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.surfapi.service.internalApiService
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtFile

class InternalApiCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(KotlinLanguage.INSTANCE),
            InternalApiCompletionProvider
        )
        extend(
            CompletionType.SMART,
            PlatformPatterns.psiElement().withLanguage(KotlinLanguage.INSTANCE),
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
            val project = position.project

            if (DumbService.isDumb(project)) {
                return
            }

            val file = position.containingFile
            if (file !is KtFile) {
                return
            }

            val module = position.module ?: return
            if (!module.hasLibrary(SurfLibraryMarker.SURF_API_CORE)) {
                return
            }

            val internalApiService = project.internalApiService()

            if (!internalApiService.isAvailableIn(module)) {
                return
            }

            val filteringSession = internalApiService.createFilteringSession(position)

            result.runRemainingContributors(parameters) { completionResult ->
                val lookupElement = completionResult.lookupElement

                if (filteringSession.shouldHide(lookupElement)) {
                    return@runRemainingContributors
                }

                result.passResult(completionResult)
            }
        }
    }
}