package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.facet.*
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.startup.ProjectActivity
import dev.slne.surf.idea.surfideaplugin.redis.facet.SurfRedisFacetType
import dev.slne.surf.idea.surfideaplugin.surfapi.facet.SurfApiFacetType

class SurfFacetSetupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val modules = project.modules

        println("Modules: ${modules.joinToString { it.name }}")

        val modelsProvider = ProjectDataManager.getInstance().createModifiableModelsProvider(project)
        try {
            for (module in modules) {
                context(module, modelsProvider) {
                    checkSurfApi()
                    checkSurfRedis()
                }
            }

            edtWriteAction {
                modelsProvider.commit()
            }
        } finally {
            edtWriteAction {
                modelsProvider.dispose()
            }
        }

        for (module in modules) {
            println(
                "Module ${module.name} done. (Has surf-redis facet: ${
                    FacetManager.getInstance(module).getFacetByType(
                        SurfRedisFacetType.ID
                    ) != null
                }, has surf-api facet: ${FacetManager.getInstance(module).getFacetByType(SurfApiFacetType.ID) != null})"
            )
        }
    }

    context(module: Module, modelsProvider: IdeModifiableModelsProvider)
    private suspend fun checkSurfApi() {
        if (SurfLibraryDetector.hasSurfApiCoreSafe(module)) {
            println("Module ${module.name} has Surf API Core. Ensuring facet...")
            ensureFacet(
                SurfApiFacetType.ID
            )
        }
    }

    context(module: Module, modelsProvider: IdeModifiableModelsProvider)
    private suspend fun checkSurfRedis() {
        if (SurfLibraryDetector.hasSurfRedisSafe(module)) {
            println("Module ${module.name} has Surf Redis. Ensuring facet...")
            ensureFacet(
                SurfRedisFacetType.ID
            )
        }
    }

    context(module: Module, modelsProvider: IdeModifiableModelsProvider)
    private suspend fun <F : Facet<C>, C : FacetConfiguration> ensureFacet(typeId: FacetTypeId<F>) {
        val facetModel = modelsProvider.getModifiableFacetModel(module)
        val facetType = FacetTypeRegistry.getInstance().findFacetType(typeId)

        if (facetModel.findFacet(typeId, facetType.defaultFacetName) != null) return

        val facet = facetType.createFacet(
            module,
            facetType.defaultFacetName,
            facetType.createDefaultConfiguration(),
            null
        )

        facetModel.addFacet(facet)
        modelsProvider.commit()
    }
}