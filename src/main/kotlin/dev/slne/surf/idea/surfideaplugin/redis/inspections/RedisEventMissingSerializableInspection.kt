package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.SurfCommonClassNames
import dev.slne.surf.idea.surfideaplugin.common.inspection.SurfApplicableInspection
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.common.util.shortNameString
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.asUnit
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classVisitor

/**
 * Verifies that classes extending Redis types are annotated with @[SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION].
 */
class RedisEventMissingSerializableInspection :
    SurfApplicableInspection<KtClass, Unit>(SurfLibraryMarker.SURF_REDIS_API) {

    private val redisBaseClassIds = setOf(
        SurfRedisClassNames.REDIS_EVENT_CLASS_ID,
        SurfRedisClassNames.REDIS_REQUEST_CLASS_ID,
        SurfRedisClassNames.REDIS_RESPONSE_CLASS_ID
    )

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isSurfApplicableByPsi(element: KtClass): Boolean {
        if (element.nameIdentifier == null) return false
        return !element.hasAnnotationPsi(SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_FQN)
    }

    override fun getApplicableRanges(element: KtClass): List<TextRange> {
        return ApplicabilityRange.single(element) { it.nameIdentifier ?: it }
    }

    override fun KaSession.prepareContext(element: KtClass): Unit? {
        val classSymbol = element.symbol as? KaClassSymbol ?: return null

        if (element.hasAnnotation(SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_ID)) {
            return null
        }

        val extendsRedisType = redisBaseClassIds.any(fun(classId): Boolean {
            val baseSymbol = findClass(classId) ?: return false
            return classSymbol.isSubClassOf(baseSymbol)
        })

        return extendsRedisType.asUnit
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtClass,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        val message = buildString {
            append("Class '")
            append(element.name ?: "Class")
            append("' extends a Redis type but is missing @")
            append(SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_FQN.shortNameString())
            append(" annotation")
        }

        return createProblemDescriptor(
            element,
            rangeInElement,
            message,
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            AddAnnotationFix(element, SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_ID).asQuickFix()
        )
    }
}