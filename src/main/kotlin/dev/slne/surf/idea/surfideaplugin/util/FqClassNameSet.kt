package dev.slne.surf.idea.surfideaplugin.util

import com.intellij.util.containers.mapSmartSet
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FqClassNameSet(vararg fqClassNames: String) {
    val classNames: Set<String> = fqClassNames.toSet()
    val fqNames: Set<FqName> = classNames.mapSmartSet { FqName(it) }
    val classIds: Set<ClassId> = fqNames.mapSmartSet { ClassId.topLevel(it) }

    operator fun contains(fqName: String): Boolean = classNames.contains(fqName)
    operator fun contains(fqName: FqName): Boolean = fqNames.contains(fqName)
    operator fun contains(classId: ClassId): Boolean = classIds.contains(classId)
    operator fun contains(annotation: KaAnnotation): Boolean = classIds.contains(annotation.classId)

    val size: Int get() = classNames.size
    fun isEmpty(): Boolean = classNames.isEmpty()
    fun isNotEmpty(): Boolean = classNames.isNotEmpty()

    operator fun plus(other: FqClassNameSet): FqClassNameSet =
        FqClassNameSet(*(classNames + other.classNames).toTypedArray())

    fun forEach(action: (FqName) -> Unit) = fqNames.forEach(action)
    fun forEachClassId(action: (ClassId) -> Unit) = classIds.forEach(action)

    fun toList(): List<String> = classNames.toList()
    fun toFqNameList(): List<FqName> = fqNames.toList()
    fun toClassIdList(): List<ClassId> = classIds.toList()

    override fun toString(): String = "FqClassNameSet($classNames)"
}