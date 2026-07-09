package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder

internal class BrowsedJarClassFinder(project: Project) : NonClasspathClassFinder(project) {

    override fun calcClassRoots(): List<VirtualFile> =
        BrowsedJarRootsService.getInstance(myProject).rootsSnapshot()
}
