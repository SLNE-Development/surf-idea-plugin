package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLock
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import org.jetbrains.kotlin.idea.base.util.module

object SurfLibraryDetector {

    const val SURF_API_CORE = "dev.slne.surf.api.core.SurfApiCore"
    const val SURF_API_PAPER = "dev.slne.surf.api.paper.SurfApiPaper"
    const val SURF_API_VELOCITY = "dev.slne.surf.api.velocity.SurfApiVelocity"

    const val SURF_REDIS_API = "dev.slne.surf.redis.RedisApi"

    const val SURF_DATABASE_API = "dev.slne.surf.database.DatabaseApi"

    const val SURF_RABBITMQ_COMMON_API = "dev.slne.surf.rabbitmq.api.RabbitMQApi"
    const val SURF_RABBITMQ_SERVER_API = "dev.slne.surf.rabbitmq.api.ServerRabbitMQApi"
    const val SURF_RABBITMQ_CLIENT_API = "dev.slne.surf.rabbitmq.api.ClientRabbitMQApi"

    @RequiresReadLock
    fun hasLibrary(element: PsiElement, marker: SurfLibraryMarker): Boolean {
        ThreadingAssertions.assertReadAccess()
        val module = element.module ?: return false
        return hasLibrary(module, marker)
    }

    @RequiresReadLock
    fun hasLibrary(module: Module, marker: SurfLibraryMarker): Boolean {
        ThreadingAssertions.assertReadAccess()
        if (module.isDisposed) return false

        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)

        return JavaPsiFacade
            .getInstance(module.project)
            .hasClass(marker.fqn, scope)
    }

    @RequiresReadLock
    fun hasAllLibraries(module: Module, markers: Collection<SurfLibraryMarker>): Boolean {
        return markers.all { hasLibrary(module, it) }
    }

    @RequiresReadLock
    fun hasAllLibraries(module: Module, markers: Array<out SurfLibraryMarker>): Boolean {
        return markers.all { hasLibrary(module, it) }
    }

    @RequiresReadLock
    fun hasAnyLibrary(module: Module, markers: Collection<SurfLibraryMarker>): Boolean {
        return markers.any { hasLibrary(module, it) }
    }

    @RequiresReadLock
    fun isClassInModuleClasspath(module: Module, fqn: String): Boolean {
        ThreadingAssertions.assertReadAccess()
        if (module.isDisposed) return false

        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val javaPsiFacade = JavaPsiFacade.getInstance(module.project)
        return javaPsiFacade.hasClass(fqn, scope)
    }

    suspend fun isClassInModuleClasspathSafe(module: Module, fqn: String) = readAction {
        isClassInModuleClasspath(module, fqn)
    }
}

@RequiresReadLock
fun Module.hasLibrary(marker: SurfLibraryMarker): Boolean = SurfLibraryDetector.hasLibrary(this, marker)

@RequiresReadLock
fun PsiElement.hasModuleLibrary(marker: SurfLibraryMarker): Boolean = SurfLibraryDetector.hasLibrary(this, marker)