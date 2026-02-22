package dev.slne.surf.idea.surfideaplugin.redis.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitorVoid

private val redisBaseClasses = setOf(
    SurfRedisClassNames.REDIS_EVENT_CLASS,
    SurfRedisClassNames.REDIS_REQUEST_CLASS,
    SurfRedisClassNames.REDIS_RESPONSE_CLASS
)

private val kotlinxSerializableFqName = FqName("kotlinx.serialization.Serializable")


class RedisEventMissingSerializableInspection : AbstractKotlinInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = object : KtVisitorVoid() {
        override fun visitClass(klass: KtClass) {
            val module = klass.module ?: return
            if (!SurfLibraryDetector.hasSurfRedis(module)) return

            val lightClass = klass.toLightClass() ?: return
            val isRedisBaseClass = redisBaseClasses.any {
                InheritanceUtil.isInheritor(lightClass, it)
            }

            if (!isRedisBaseClass) return
            val hasSerializable = lightClass.hasAnnotation("kotlinx.serialization.Serializable")

            if (!hasSerializable) {
                holder.registerProblem(
                    klass.nameIdentifier ?: klass,
                    "Class '${klass.name}' extends a Redis type but is missing @Serializable annotation",
                    ProblemHighlightType.GENERIC_ERROR,
                    AddSerializableQuickFix()
                )
            }
        }
    }

    private class AddSerializableQuickFix : LocalQuickFix {
        override fun getName(): String = "Add @Serializable annotation"
        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val klass = descriptor.psiElement.parent as? KtClass ?: return
            val classId = ClassId.topLevel(kotlinxSerializableFqName)

            klass.addAnnotation(classId)
        }
    }
}