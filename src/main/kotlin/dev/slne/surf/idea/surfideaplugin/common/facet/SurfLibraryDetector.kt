package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.base.util.module

object SurfLibraryDetector {

    const val SURF_API_CORE = "dev.slne.surf.surfapi.core.api.SurfCoreApi"
    const val SURF_API_PAPER = "dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi"
    const val SURF_API_VELOCITY = "dev.slne.surf.surfapi.velocity.api.SurfVelocityApi"

    const val SURF_REDIS_API = "dev.slne.surf.redis.RedisApi"

    const val SURF_DATABASE_API = "dev.slne.surf.database.DatabaseApi"

    const val SURF_RABBITMQ_COMMON_API = "dev.slne.surf.rabbitmq.api.RabbitMqApi"
    const val SURF_RABBITMQ_SERVER_API = "dev.slne.surf.rabbitmq.api.ServerRabbitMQApi"
    const val SURF_RABBITMQ_CLIENT_API = "dev.slne.surf.rabbitmq.api.ClientRabbitMQApi"

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

    fun hasSurfApiCore(module: Module) = isClassInModuleClasspath(module, SURF_API_CORE)
    suspend fun hasSurfApiCoreSafe(module: Module) = isClassInModuleClasspathSafe(module, SURF_API_CORE)

    fun hasSurfApiPaper(module: Module) = isClassInModuleClasspath(module, SURF_API_PAPER)
    suspend fun hasSurfApiPaperSafe(module: Module) = isClassInModuleClasspathSafe(module, SURF_API_PAPER)

    fun hasSurfApiVelocity(module: Module) = isClassInModuleClasspath(module, SURF_API_VELOCITY)
    suspend fun hasSurfApiVelocitySafe(module: Module) = isClassInModuleClasspathSafe(module, SURF_API_VELOCITY)

    fun hasSurfRedis(module: Module) = isClassInModuleClasspath(module, SURF_REDIS_API)
    suspend fun hasSurfRedisSafe(module: Module) = isClassInModuleClasspathSafe(module, SURF_REDIS_API)

    fun hasSurfRedis(psiElement: PsiElement): Boolean = hasSurfRedis(psiElement.module ?: return false)
    suspend fun hasSurfRedisSafe(psiElement: PsiElement): Boolean = hasSurfRedisSafe(psiElement.module ?: return false)

    fun hasSurfDatabase(module: Module) = isClassInModuleClasspath(module, SURF_DATABASE_API)
    suspend fun hasSurfDatabaseSafe(module: Module) = isClassInModuleClasspathSafe(module, SURF_DATABASE_API)

    fun hasSurfDatabase(psiElement: PsiElement): Boolean = hasSurfDatabase(psiElement.module ?: return false)
    suspend fun hasSurfDatabaseSafe(psiElement: PsiElement): Boolean =
        hasSurfDatabaseSafe(psiElement.module ?: return false)

    fun hasSurfRabbitMqCommon(module: Module) = isClassInModuleClasspath(module, SURF_RABBITMQ_COMMON_API)
    suspend fun hasSurfRabbitMqCommonSafe(module: Module) =
        isClassInModuleClasspathSafe(module, SURF_RABBITMQ_COMMON_API)

    fun hasSurfRabbitMqServer(module: Module) = isClassInModuleClasspath(module, SURF_RABBITMQ_SERVER_API)
    suspend fun hasSurfRabbitMqServerSafe(module: Module) =
        isClassInModuleClasspathSafe(module, SURF_RABBITMQ_SERVER_API)

    fun hasSurfRabbitMqClient(module: Module) = isClassInModuleClasspath(module, SURF_RABBITMQ_CLIENT_API)
    suspend fun hasSurfRabbitMqClientSafe(module: Module) =
        isClassInModuleClasspathSafe(module, SURF_RABBITMQ_CLIENT_API)
}