package dev.slne.surf.idea.surfideaplugin.common.service

import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

@Suppress("UnstableApiUsage")
@Service(Service.Level.PROJECT)
class InheritanceService(
    private val project: Project
) {

    suspend fun implementInterfaceIfMissing(target: KtClassOrObject, interfaceId: ClassId) {
        if (implementsInterface(target, interfaceId)) return
        implementInterface(target, interfaceId)
    }

    suspend fun implementInterface(target: KtClassOrObject, interfaceId: ClassId) {
        writeCommandAction(project, "Implement ${interfaceId.shortClassName}") {
            val entry = target.addSuperTypeListEntry(
                KtPsiFactory(project)
                    .createSuperTypeEntry(interfaceId.asFqNameString())
            )
            ShortenReferencesFacility.getInstance().shorten(entry)
        }
    }

    suspend fun implementsInterface(target: KtClassOrObject, interfaceId: ClassId): Boolean {
        try {
            return readAction {
                analyze(target) {
                    val classSymbol = target.classSymbol ?: return@analyze false
                    classSymbol.defaultType.isSubtypeOf(interfaceId)
                }
            }
        } catch (e: Exception) {
            if (Logger.shouldRethrow(e)) throw e
            throw KotlinExceptionWithAttachments("Unable to check if the class is an interface", e)
                .withPsiAttachment("target.kt", target)
                .withAttachment("interfaceId.txt", interfaceId)
        }
    }

    companion object {
        fun getInstance(project: Project): InheritanceService = project.service()
    }
}

fun Project.inheritanceService(): InheritanceService = InheritanceService.getInstance(this)