package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import dev.slne.surf.idea.surfideaplugin.SurfBundle
import org.jetbrains.kotlin.psi.KtFile
import java.util.function.Function
import javax.swing.JComponent

internal class KotlinClassDecompileNotificationProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension != "class") return null
        if (ProjectFileIndex.getInstance(project).isInLibraryClasses(file)) return null
        val psiFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null
        if (!psiFile.isCompiled) return null

        return Function { fileEditor ->
            val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info)
            panel.text = SurfBundle.message("jar.viewer.kotlin.class.banner.text")
            panel.createActionLabel(SurfBundle.message("jar.viewer.kotlin.class.decompile.action")) {
                JarClassDecompiler.openDecompiledJava(project, file)
            }
            panel
        }
    }
}
