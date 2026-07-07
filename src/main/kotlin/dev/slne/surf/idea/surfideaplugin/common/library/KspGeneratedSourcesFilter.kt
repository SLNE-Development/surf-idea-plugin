package dev.slne.surf.idea.surfideaplugin.common.library

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.vfs.VirtualFile

/**
 * Marks KSP output (e.g. the generated `<Service>ClientImpl`/`<Service>Descriptor` classes of
 * the surf-rabbitmq RPC processor) as generated sources. This feeds the editor's
 * "generated source" banner, the Find Usages "usages in generated code" filter, and
 * refactoring safety checks.
 */
class KspGeneratedSourcesFilter : GeneratedSourcesFilter() {

    override fun isGeneratedSource(file: VirtualFile, project: Project): Boolean {
        return file.path.contains("/build/generated/ksp/")
    }
}
