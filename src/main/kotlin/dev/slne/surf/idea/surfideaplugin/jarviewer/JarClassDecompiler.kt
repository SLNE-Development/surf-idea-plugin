package dev.slne.surf.idea.surfideaplugin.jarviewer

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.testFramework.LightVirtualFile
import dev.slne.surf.idea.surfideaplugin.SurfBundle
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.io.File
import java.util.jar.Manifest

private val DECOMPILED_JAR_CLASS = Key.create<Boolean>("surf.jarviewer.decompiled.class")

internal val VirtualFile.isDecompiledJarClass: Boolean
    get() = getUserData(DECOMPILED_JAR_CLASS) == true

internal object JarClassDecompiler {

    private val log = logger<JarClassDecompiler>()

    fun openDecompiledJava(project: Project, classFile: VirtualFile) {
        object : Task.Backgroundable(
            project,
            SurfBundle.message("jar.viewer.decompile.progress", classFile.name),
        ) {
            private var decompiledText: String? = null

            override fun run(indicator: ProgressIndicator) {
                decompiledText = decompile(classFile)
            }

            override fun onSuccess() {
                val text = decompiledText
                if (text == null) {
                    Messages.showErrorDialog(
                        project,
                        SurfBundle.message("jar.viewer.decompile.failed.text", classFile.name),
                        SurfBundle.message("jar.viewer.decompile.failed.title"),
                    )
                    return
                }

                (classFile.fileSystem as? ArchiveFileSystem)
                    ?.getRootByEntry(classFile)
                    ?.let { BrowsedJarRootsService.getInstance(project).register(it) }

                val file = LightVirtualFile(
                    classFile.nameWithoutExtension + ".decompiled.java",
                    JavaFileType.INSTANCE,
                    text,
                )
                file.isWritable = false
                file.putUserData(DECOMPILED_JAR_CLASS, true)
                OpenFileDescriptor(project, file).navigate(true)
            }
        }.queue()
    }

    private fun decompile(classFile: VirtualFile): String? {
        val innerClassPrefix = classFile.nameWithoutExtension + "$"
        val classFiles = listOf(classFile) + classFile.parent?.children.orEmpty().filter {
            it.extension == "class" && it.nameWithoutExtension.startsWith(innerClassPrefix)
        }
        val bytecode = classFiles.associate {
            File(it.path) to it.contentsToByteArray(false)
        }

        val saver = TextResultSaver()
        val decompiler = BaseDecompiler(
            { externalPath, _ -> bytecode[File(FileUtil.toSystemIndependentName(externalPath))] },
            saver,
            mapOf<String, Any>(IFernflowerPreferences.REMOVE_BRIDGE to "0"),
            SilentLogger(),
        )
        bytecode.keys.forEach(decompiler::addSource)
        return try {
            decompiler.decompileContext()
            saver.resultText.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            log.warn("Failed to decompile ${classFile.path}", e)
            null
        }
    }

    private class TextResultSaver : IResultSaver {
        private val decompiledText = linkedMapOf<String, String>()

        val resultText: String
            get() {
                decompiledText.values.singleOrNull()?.let { return it }
                return buildString {
                    for ((name, content) in decompiledText) {
                        appendLine("// $name")
                        append(content)
                    }
                }
            }

        override fun saveClassFile(
            path: String?,
            qualifiedName: String?,
            entryName: String?,
            content: String?,
            mapping: IntArray?,
        ) {
            if (content != null) {
                decompiledText[entryName ?: qualifiedName.orEmpty()] = content
            }
        }

        override fun saveFolder(path: String?) = Unit
        override fun copyFile(source: String?, path: String?, entryName: String?) = Unit
        override fun createArchive(path: String?, archiveName: String?, manifest: Manifest?) = Unit
        override fun saveDirEntry(path: String?, archiveName: String?, entryName: String?) = Unit
        override fun copyEntry(source: String?, path: String?, archiveName: String?, entry: String?) = Unit
        override fun saveClassEntry(
            path: String?,
            archiveName: String?,
            qualifiedName: String?,
            entryName: String?,
            content: String?,
        ) = Unit

        override fun closeArchive(path: String?, archiveName: String?) = Unit
    }

    private class SilentLogger : IFernflowerLogger() {
        override fun writeMessage(message: String?, severity: Severity?) = Unit
        override fun writeMessage(message: String?, severity: Severity?, t: Throwable?) = Unit
    }
}
