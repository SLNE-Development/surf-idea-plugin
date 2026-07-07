package dev.slne.surf.idea.surfideaplugin.surfapi.service

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.psi.KtDeclaration
import kotlin.collections.mutableMapOf

@Service(Service.Level.PROJECT)
class InternalApiService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): InternalApiService = project.service()
    }

    private val projectFileIndex: ProjectFileIndex
        get() = ProjectFileIndex.getInstance(project)

    fun isAvailableIn(module: Module): Boolean {
        return SurfLibraryDetector.hasClass(
            module,
            SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION,
        )
    }

    fun createFilteringSession(): InternalApiFilteringSession {
        return InternalApiFilteringSession()
    }

    inner class InternalApiFilteringSession {
        private val annotationHasInternalApiMarkerCache = mutableMapOf<String, Boolean>()

        fun shouldHide(lookupElement: LookupElement): Boolean {
            val psiElement = lookupElement.psiElement ?: return false
            val declaration = psiElement as? KtDeclaration ?: return false

            if (!isFromLibrary(declaration)) {
                return false
            }

            return analyze(declaration) {
                val symbol = declaration.symbol

                isOrContainedInInternalApi(
                    symbol = symbol,
                    annotationHasInternalApiMarkerCache = annotationHasInternalApiMarkerCache,
                )
            }
        }

        private fun isFromLibrary(element: PsiElement): Boolean {
            val virtualFile = element.containingFile?.virtualFile ?: return true

            if (projectFileIndex.isInSourceContent(virtualFile)) {
                return false
            }

            return projectFileIndex.isInLibraryClasses(virtualFile) || projectFileIndex.isInLibrarySource(virtualFile)
        }
    }

    context(_: KaSession)
    fun isHiddenInternalApi(symbol: KaDeclarationSymbol, useSiteElement: PsiElement): Boolean {
        if (!isFromLibrary(symbol, useSiteElement)) return false
        return isOrContainedInInternalApi(
            symbol = symbol,
            annotationHasInternalApiMarkerCache = mutableMapOf(),
        )
    }

    private fun isFromLibrary(symbol: KaSymbol, useSiteElement: PsiElement): Boolean {
        val psi = symbol.psi ?: return true
        val virtualFile = psi.containingFile?.virtualFile ?: return true
        val fileIndex = ProjectFileIndex.getInstance(useSiteElement.project)

        if (fileIndex.isInSourceContent(virtualFile)) {
            return false
        }

        return fileIndex.isInLibraryClasses(virtualFile) || fileIndex.isInLibrarySource(virtualFile)
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun isOrContainedInInternalApi(
        symbol: KaDeclarationSymbol,
        annotationHasInternalApiMarkerCache: MutableMap<String, Boolean>,
    ): Boolean {
        if (hasInternalApiMarkerAnnotation(symbol, annotationHasInternalApiMarkerCache)) {
            return true
        }

        val containingSymbol = symbol.containingDeclaration ?: return false

        return isOrContainedInInternalApi(
            symbol = containingSymbol,
            annotationHasInternalApiMarkerCache = annotationHasInternalApiMarkerCache,
        )
    }


    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun hasInternalApiMarkerAnnotation(
        symbol: KaDeclarationSymbol,
        annotationHasInternalApiMarkerCache: MutableMap<String, Boolean>,
    ): Boolean {
        for (annotation in symbol.annotations) {
            val annotationClassId = annotation.classId ?: continue

            if (annotationClassId == SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION_ID) {
                return true
            }

            val annotationClassIdString = annotationClassId.asSingleFqName().asString()

            val hasMarker = annotationHasInternalApiMarkerCache.computeIfAbsent(annotationClassIdString) {
                val annotationClassSymbol = findClass(annotationClassId) ?: return@computeIfAbsent false

                annotationClassSymbol.annotations.any { metaAnnotation ->
                    metaAnnotation.classId == SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION_ID
                }
            }

            if (hasMarker) {
                return true
            }
        }

        return false
    }
}

fun Project.internalApiService(): InternalApiService = InternalApiService.getInstance(this)