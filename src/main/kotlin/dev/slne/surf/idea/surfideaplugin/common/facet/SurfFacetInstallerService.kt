package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import dev.slne.surf.idea.surfideaplugin.rabbitmq.facet.SurfRabbitFacetType
import dev.slne.surf.idea.surfideaplugin.redis.facet.SurfRedisFacetType
import dev.slne.surf.idea.surfideaplugin.surfapi.facet.SurfApiFacetType
import dev.slne.surf.idea.surfideaplugin.surfapi.platform.SurfApiPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.EnumSet

@Service(Service.Level.PROJECT)
class SurfFacetInstallerService(private val project: Project, private val scope: CoroutineScope) {

    fun installFacets() {
        scope.launch {
            installFacetsSuspend()
        }
    }

    suspend fun installFacetsSuspend() {
        val modules = project.modules
        val modelsProvider = ProjectDataManager.getInstance().createModifiableModelsProvider(project)

        try {
            for (module in modules) {
                // --- SurfApi ---
                val hasSurfApi = smartReadAction(project) { SurfLibraryDetector.hasSurfApiCore(module) }
                val hasPaper   = smartReadAction(project) { SurfLibraryDetector.hasSurfApiPaper(module) }
                val hasVelocity = smartReadAction(project) { SurfLibraryDetector.hasSurfApiVelocity(module) }

                // --- Redis / RabbitMQ ---
                val hasSurfRedis   = smartReadAction(project) { SurfLibraryDetector.hasSurfRedis(module) }
                val hasSurfRabbitMq = smartReadAction(project) { SurfLibraryDetector.hasSurfRabbitMqCommon(module) }

                context(module, modelsProvider) {
                    if (hasSurfApi) {
                        ensureFacet(SurfApiFacetType.ID) { config ->
                            val platforms = EnumSet.of(SurfApiPlatform.CORE)
                            if (hasPaper) platforms.add(SurfApiPlatform.PAPER)
                            if (hasVelocity) platforms.add(SurfApiPlatform.VELOCITY)
                            config.detectedPlatforms = platforms
                        }
                    }

                    if (hasSurfRedis) ensureFacet(SurfRedisFacetType.ID)
                    if (hasSurfRabbitMq) ensureFacet(SurfRabbitFacetType.ID)
                }
            }

            edtWriteAction { modelsProvider.commit() }
        } finally {
            edtWriteAction { modelsProvider.dispose() }
        }
    }

    context(module: Module, modelsProvider: IdeModifiableModelsProvider)
    private fun <F : Facet<C>, C : FacetConfiguration> ensureFacet(typeId: FacetTypeId<F>, configure: ((C) -> Unit)? = null) {
        val facetModel = modelsProvider.getModifiableFacetModel(module)
        val facetType = FacetTypeRegistry.getInstance().findFacetType(typeId)

        val existing = facetModel.findFacet(typeId, facetType.defaultFacetName)
        if (existing != null) {
            configure?.invoke(existing.configuration)
            return
        }

        val config = facetType.createDefaultConfiguration()
        configure?.invoke(config)

        val facet = facetType.createFacet(
            module,
            facetType.defaultFacetName,
            config,
            null
        )
        facetModel.addFacet(facet)
    }

    companion object {
        fun getInstance(project: Project): SurfFacetInstallerService = project.service()
    }
}

fun Project.surfFacetInstallerService(): SurfFacetInstallerService = SurfFacetInstallerService.getInstance(this)