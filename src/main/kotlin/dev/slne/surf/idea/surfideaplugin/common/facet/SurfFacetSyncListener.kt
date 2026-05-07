package dev.slne.surf.idea.surfideaplugin.common.facet

import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project

class SurfFacetSyncListener(private val project: Project) : ProjectDataImportListener {
    override fun onImportFinished(projectPath: String?) {
        project.surfFacetInstallerService().installFacets()
    }
}