package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class SurfFacetSetupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.surfFacetInstallerService().installFacets()
    }
}