@file:OptIn(KaContextParameterApi::class)

package dev.slne.surf.idea.surfideaplugin.common.util

import dev.slne.surf.idea.surfideaplugin.util.FqClassNameSet
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.components.isSubClassOf
import org.jetbrains.kotlin.analysis.api.components.isSubtypeOf
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.namedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

context(_: KaSession)
fun KtNamedFunction.hasAnnotation(annotationId: ClassId): Boolean {
    return symbol.annotations.any { it.classId == annotationId }
}

context(_: KaSession)
fun KtNamedFunction.hasAnyAnnotation(annotationIds: Iterable<ClassId>): Boolean {
    val ids = annotationIds.toSet()
    if (ids.isEmpty()) return false

    return symbol.annotations.any { it.classId in ids }
}

context(_: KaSession)
fun KaCallableSymbol.hasAnyAnnotation(annotations: FqClassNameSet): Boolean {
    return this.annotations.any { it.classId in annotations.classIds }
}

context(_: KaSession)
fun KtClassOrObject.hasAnnotation(annotationId: ClassId): Boolean {
    return symbol.annotations.any { it.classId == annotationId }
}

context(_: KaSession)
fun KtNamedFunction.findFirstAnnotation(annotationIds: Set<ClassId>): KaAnnotation? {
    return symbol.annotations.find { annotationIds.contains(it.classId) }
}

context(_: KaSession)
fun KtNamedFunction.findValueParameter(annotation: ClassId): KtParameter? {
    for (parameter in valueParameters) {
        val type = parameter.symbol.returnType as? KaClassType ?: continue
        if (type.classId == annotation) return parameter
    }

    return null
}

context(_: KaSession)
fun KtClassOrObject.isSubClassOf(classId: ClassId): Boolean {
    val symbol = namedClassSymbol ?: return false
    val superSymbol = findClass(classId) ?: return false

    return symbol.isSubClassOf(superSymbol)
}

context(_: KaSession)
fun KtParameter.isReturnType(classId: ClassId): Boolean {
    return symbol.returnType.isSubtypeOf(classId)
}