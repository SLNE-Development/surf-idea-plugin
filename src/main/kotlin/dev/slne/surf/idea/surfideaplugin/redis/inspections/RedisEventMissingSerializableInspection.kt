package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.KotlinApplicableInspectionBase
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.ApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.asQuickFix
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.classVisitor


class RedisEventMissingSerializableInspection : KotlinApplicableInspectionBase<KtClass, Unit>() {
    private val serializableFqName = FqName("kotlinx.serialization.Serializable")
    private val serializableClassId = ClassId.topLevel(serializableFqName)

    private val redisBaseClassIds = setOf(
        SurfRedisClassNames.REDIS_EVENT_CLASS,
        SurfRedisClassNames.REDIS_REQUEST_CLASS,
        SurfRedisClassNames.REDIS_RESPONSE_CLASS
    ).map { ClassId.topLevel(FqName(it)) }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): KtVisitor<*, *> = classVisitor { element ->
        visitTargetElement(element, holder, isOnTheFly)
    }

    override fun isApplicableByPsi(element: KtClass): Boolean {
        if (!SurfLibraryDetector.hasSurfRedis(element)) return false
        if (KotlinPsiHeuristics.hasAnnotation(element, serializableFqName)) return false
        return true
    }

    override fun getApplicableRanges(element: KtClass): List<TextRange> {
        return ApplicabilityRange.single(element) { it.nameIdentifier ?: it }
    }

    override fun KaSession.prepareContext(element: KtClass): Unit? {
        val classSymbol = element.symbol as? KaClassSymbol ?: return null
        val hasSerializable = classSymbol.annotations.any { annotation ->
            annotation.classId == serializableClassId
        }

        if (hasSerializable) return null

        val isRedisSubtype = redisBaseClassIds.any { baseClassId ->
            val baseSymbol = findClass(baseClassId) ?: return@any false
            classSymbol.isSubClassOf(baseSymbol)
        }

        return if (isRedisSubtype) Unit else null
    }

    override fun InspectionManager.createProblemDescriptor(
        element: KtClass,
        context: Unit,
        rangeInElement: TextRange?,
        onTheFly: Boolean
    ): ProblemDescriptor {
        return createProblemDescriptor(
            element,
            rangeInElement,
            "Class '${element.name}' extends a Redis type but is missing @Serializable annotation",
            ProblemHighlightType.GENERIC_ERROR,
            onTheFly,
            AddAnnotationFix(element, serializableClassId).asQuickFix()
        )
    }
}