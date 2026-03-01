package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.smartReadAction
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
        val modelsProvider = ProjectDataManager.getInstance().createModifiableModelsProvider(project)

        try {
            for (module in modules) {
                val hasSurfApi = smartReadAction(project) {
                    SurfLibraryDetector.hasSurfApiCore(module)
                }
                val hasSurfRedis = smartReadAction(project) {
                    SurfLibraryDetector.hasSurfRedis(module)
                }

                context(module, modelsProvider) {
                    if (hasSurfApi) ensureFacet(SurfApiFacetType.ID)
                    if (hasSurfRedis) ensureFacet(SurfRedisFacetType.ID)
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
    }

    context(module: Module, modelsProvider: IdeModifiableModelsProvider)
    private fun <F : Facet<C>, C : FacetConfiguration> ensureFacet(typeId: FacetTypeId<F>) {
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
    }
}