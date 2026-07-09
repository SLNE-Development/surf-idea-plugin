package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.psi.search.SearchScope

internal class BrowsedJarResolveScopeProvider : ResolveScopeProvider() {

    override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? {
        if (!file.isDecompiledJarClass) return null
        val allScope = GlobalSearchScope.allScope(project)
        val roots = BrowsedJarRootsService.getInstance(project).rootsSnapshot()
        if (roots.isEmpty()) return allScope
        return allScope.uniteWith(NonClasspathDirectoriesScope.compose(roots))
    }
}

internal class BrowsedJarResolveScopeEnlarger : ResolveScopeEnlarger() {

    override fun getAdditionalResolveScope(file: VirtualFile, project: Project): SearchScope? {
        val service = BrowsedJarRootsService.getInstance(project)
        if (service.isEmpty()) return null
        val roots = service.rootsSnapshot()
        if (roots.none { VfsUtilCore.isAncestor(it, file, false) }) return null
        return NonClasspathDirectoriesScope.compose(roots)
    }
}
