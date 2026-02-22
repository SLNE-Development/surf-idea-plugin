package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.util.module

object SurfLibraryDetector {

    const val SURF_API_CORE = "dev.slne.surf.surfapi.core.api.SurfCoreApi"
    const val SURF_API_PAPER = "dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi"
    const val SURF_API_VELOCITY = "dev.slne.surf.surfapi.velocity.api.SurfVelocityApi"

    const val SURF_REDIS_API = "dev.slne.surf.redis.RedisApi"

    const val SURF_DATABASE_API = "dev.slne.surf.database.DatabaseApi"

    fun isClassInModuleClasspath(module: Module, fqn: String): Boolean {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        val javaPsiFacade = JavaPsiFacade.getInstance(module.project)
        return javaPsiFacade.hasClass(fqn, scope)
    }

    fun hasSurfApiCore(module: Module) = isClassInModuleClasspath(module, SURF_API_CORE)
    fun hasSurfApiPaper(module: Module) = isClassInModuleClasspath(module, SURF_API_PAPER)
    fun hasSurfApiVelocity(module: Module) = isClassInModuleClasspath(module, SURF_API_VELOCITY)
    fun hasSurfRedis(module: Module) = isClassInModuleClasspath(module, SURF_REDIS_API)
    fun hasSurfRedis(psiElement: PsiElement): Boolean = hasSurfRedis(psiElement.module ?: return false)
    fun hasSurfDatabase(module: Module) = isClassInModuleClasspath(module, SURF_DATABASE_API)
}