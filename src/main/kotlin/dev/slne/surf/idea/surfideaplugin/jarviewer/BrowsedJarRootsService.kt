package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
internal class BrowsedJarRootsService(private val project: Project) {

    private val roots = ConcurrentHashMap.newKeySet<VirtualFile>()

    fun isEmpty(): Boolean = roots.isEmpty()

    fun rootsSnapshot(): List<VirtualFile> = roots.filter { it.isValid }

    fun register(jarRoot: VirtualFile) {
        if (!roots.add(jarRoot)) return
        PsiElementFinder.EP.getExtensions(project)
            .filterIsInstance<BrowsedJarClassFinder>()
            .forEach { it.clearCache() }

        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            ApplicationManager.getApplication().runWriteAction {
                ProjectRootManagerEx.getInstanceEx(project)
                    .makeRootsChange({}, RootsChangeRescanningInfo.NO_RESCAN_NEEDED)
            }
        }
    }

    companion object {
        fun getInstance(project: Project): BrowsedJarRootsService = project.service()
    }
}
