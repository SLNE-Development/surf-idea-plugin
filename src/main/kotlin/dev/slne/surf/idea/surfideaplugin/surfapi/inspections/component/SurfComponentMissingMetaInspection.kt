package dev.slne.surf.idea.surfideaplugin.surfapi.inspections.component

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.isSubClassOf
import dev.slne.surf.idea.surfideaplugin.surfapi.SurfApiClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classOrObjectVisitor

/**
 * `AbstractComponent` requires a `@SurfComponentMeta` (meta-)annotated annotation
 * (e.g. `@Service`, `@Repository`) on the concrete component class — its init block
 * throws otherwise. Reported here instead of at first instantiation.
 */
class SurfComponentMissingMetaInspection :
    SurfApplicableInspection<KtClassOrObject, Unit>(SurfLibraryMarker.SURF_API_CORE) {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classOrObjectVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtClassOrObject): Boolean {
        if (element.nameIdentifier == null) return false
        if (element is KtClass) {
            if (element.isInterface() || element.isAnnotation() || element.isEnum()) return false
            if (element.hasModifier(KtTokens.ABSTRACT_KEYWORD) || element.hasModifier(KtTokens.SEALED_KEYWORD)) {
                return false
            }
        }
        return element.superTypeListEntries.isNotEmpty()
    }

    override fun KaSession.prepareContext(element: KtClassOrObject): Unit? {
        if (!element.isSubClassOf(SurfApiClassNames.ABSTRACT_COMPONENT_CLASS_ID)) return null

        val hasComponentMeta = element.symbol.annotations.any { annotation ->
            val annotationClassId = annotation.classId ?: return@any false

            if (annotationClassId == SurfApiClassNames.SURF_COMPONENT_META_ANNOTATION_ID) {
                return@any true
            }

            val annotationClass = findClass(annotationClassId) ?: return@any false
            annotationClass.annotations.any { metaAnnotation ->
                metaAnnotation.classId == SurfApiClassNames.SURF_COMPONENT_META_ANNOTATION_ID
            }
        }

        return if (hasComponentMeta) null else Unit
    }

    override fun getApplicableRanges(element: KtClassOrObject): List<TextRange> {
        return ApplicabilityRanges.declarationName(element)
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtClassOrObject,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Component class must be annotated with a @SurfComponentMeta component annotation (e.g. @Service or @Repository)",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
        )
    }
}
