package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

private val SUPPORTED_ARCHIVE_EXTENSIONS = setOf("jar", "war", "zip")

private fun VirtualFile.isLocalArchive(): Boolean =
    !isDirectory && isInLocalFileSystem && extension?.lowercase() in SUPPORTED_ARCHIVE_EXTENSIONS

internal class JarContentTreeStructureProvider : TreeStructureProvider, DumbAware {

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings?,
    ): Collection<AbstractTreeNode<*>> {
        if (children.none { it.isReplaceableArchiveNode() }) return children
        return children.map { child ->
            if (child.isReplaceableArchiveNode()) {
                val fileNode = child as PsiFileNode
                val psiFile = fileNode.value ?: return@map child
                ExpandableArchiveNode(fileNode.project, psiFile, settings)
            } else {
                child
            }
        }
    }

    private fun AbstractTreeNode<*>.isReplaceableArchiveNode(): Boolean =
        this is PsiFileNode && this !is ExpandableArchiveNode && virtualFile?.isLocalArchive() == true
}

private class ExpandableArchiveNode(
    project: Project?,
    psiFile: PsiFile,
    settings: ViewSettings?,
) : PsiFileNode(project, psiFile, settings) {

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>>? {
        val file = virtualFile ?: return emptyList()
        val jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(file) ?: return emptyList()
        // make the archive's classes resolvable while the user is browsing it
        project?.let { BrowsedJarRootsService.getInstance(it).register(jarRoot) }
        return jarRoot.children.orEmpty().map { ArchiveEntryNode(project, it, settings) }
    }
}

private class ArchiveEntryNode(
    project: Project?,
    entry: VirtualFile,
    private val viewSettings: ViewSettings?,
) : ProjectViewNode<VirtualFile>(project, entry, viewSettings) {

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val entry = value ?: return emptyList()
        if (!entry.isDirectory) return emptyList()
        return entry.children.orEmpty().map { ArchiveEntryNode(project, it, viewSettings) }
    }

    override fun update(presentation: PresentationData) {
        val entry = value ?: return
        presentation.presentableText = entry.name
        presentation.setIcon(
            if (entry.isDirectory) {
                AllIcons.Nodes.Folder
            } else {
                FileTypeRegistry.getInstance().getFileTypeByFileName(entry.nameSequence).icon
                    ?: AllIcons.FileTypes.Any_type
            }
        )
    }

    override fun contains(file: VirtualFile): Boolean {
        val entry = value ?: return false
        return VfsUtilCore.isAncestor(entry, file, false)
    }

    override fun getVirtualFile(): VirtualFile? = value

    override fun canNavigate(): Boolean = value?.isDirectory == false

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun navigate(requestFocus: Boolean) {
        val entry = value ?: return
        val project = project ?: return
        if (!entry.isDirectory) {
            FileEditorManager.getInstance(project).openFile(entry, requestFocus)
        }
    }

    override fun isAlwaysLeaf(): Boolean = value?.isDirectory != true

    override fun getWeight(): Int = if (value?.isDirectory == true) 0 else 20
}
