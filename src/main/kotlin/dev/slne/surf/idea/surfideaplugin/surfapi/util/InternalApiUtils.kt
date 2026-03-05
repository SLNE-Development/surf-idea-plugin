package dev.slne.surf.idea.surfideaplugin.surfapi.util

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object InternalApiUtils {
    private val INTERNAL_API_MARKER_CLASS_ID =
        ClassId.topLevel(FqName(SurfApiClassNames.INTERNAL_API_MARKER_ANNOTATION))


    context(_: KaSession)
    fun isHiddenInternalApi(symbol: KaDeclarationSymbol, useSiteElement: PsiElement): Boolean {
        if (!isFromLibrary(symbol, useSiteElement)) return false
        return isOrContainedInInternalApi(symbol)
    }

    private fun isFromLibrary(symbol: KaSymbol, useSiteElement: PsiElement): Boolean {
        val psi = symbol.psi ?: return true

        val virtualFile = psi.containingFile?.virtualFile ?: return true
        val project = useSiteElement.project
        val fileIndex = ProjectFileIndex.getInstance(project)

        if (fileIndex.isInSourceContent(virtualFile)) return false
        return fileIndex.isInLibraryClasses(virtualFile) || fileIndex.isInLibrarySource(virtualFile)
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun isOrContainedInInternalApi(symbol: KaDeclarationSymbol): Boolean {
        if (hasInternalApiMarkerAnnotation(symbol)) return true

        val containingSymbol = symbol.containingDeclaration
        if (containingSymbol != null) {
            return isOrContainedInInternalApi(containingSymbol)
        }

        return false
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun hasInternalApiMarkerAnnotation(symbol: KaDeclarationSymbol): Boolean {

        for (annotation in symbol.annotations) {
            val annotationClassId = annotation.classId ?: continue
            val annotationClassSymbol = findClass(annotationClassId) ?: continue
            val hasMarker = annotationClassSymbol.annotations.any { metaAnnotation ->
                metaAnnotation.classId == INTERNAL_API_MARKER_CLASS_ID
            }

            if (hasMarker) return true
        }

        return false
    }
}