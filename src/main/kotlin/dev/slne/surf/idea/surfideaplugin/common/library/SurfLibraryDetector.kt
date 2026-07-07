package dev.slne.surf.idea.surfideaplugin.common.library

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.base.util.module
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects the presence of Surf libraries on a module's classpath.
 *
 * Results are cached per module and invalidated on project root changes, so callers
 * (inspections, line markers, completion) may query freely per element without paying
 * an index lookup each time.
 */
object SurfLibraryDetector {

    private val CACHE_KEY: Key<CachedValue<ConcurrentHashMap<String, Boolean>>> =
        Key.create("dev.slne.surf.idea.surfideaplugin.libraryCache")

    @RequiresReadLock
    fun hasLibrary(element: PsiElement, marker: SurfLibraryMarker): Boolean {
        val module = element.module ?: return false
        return hasLibrary(module, marker)
    }

    @RequiresReadLock
    fun hasLibrary(module: Module, marker: SurfLibraryMarker): Boolean = hasClass(module, marker.fqn)

    @RequiresReadLock
    fun hasAllLibraries(module: Module, markers: Collection<SurfLibraryMarker>): Boolean {
        return markers.all { hasLibrary(module, it) }
    }

    @RequiresReadLock
    fun hasAllLibraries(module: Module, markers: Array<out SurfLibraryMarker>): Boolean {
        return markers.all { hasLibrary(module, it) }
    }

    @RequiresReadLock
    fun hasAnyLibrary(module: Module, markers: Collection<SurfLibraryMarker>): Boolean {
        return markers.any { hasLibrary(module, it) }
    }

    /**
     * Whether a class with the given fully-qualified name is on the module's classpath
     * (module dependencies and libraries, without test scope).
     */
    @RequiresReadLock
    fun hasClass(module: Module, fqn: String): Boolean {
        ThreadingAssertions.assertReadAccess()
        if (module.isDisposed) return false

        return classpathCache(module).computeIfAbsent(fqn) { resolveHasClass(module, it) }
    }

    private fun classpathCache(module: Module): ConcurrentHashMap<String, Boolean> {
        return CachedValuesManager.getManager(module.project).getCachedValue(
            module,
            CACHE_KEY,
            {
                CachedValueProvider.Result.create(
                    ConcurrentHashMap(),
                    ProjectRootModificationTracker.getInstance(module.project),
                )
            },
            false,
        )
    }

    private fun resolveHasClass(module: Module, fqn: String): Boolean {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        return JavaPsiFacade.getInstance(module.project).hasClass(fqn, scope)
    }
}

@RequiresReadLock
fun Module.hasLibrary(marker: SurfLibraryMarker): Boolean = SurfLibraryDetector.hasLibrary(this, marker)

@RequiresReadLock
fun PsiElement.hasModuleLibrary(marker: SurfLibraryMarker): Boolean = SurfLibraryDetector.hasLibrary(this, marker)
