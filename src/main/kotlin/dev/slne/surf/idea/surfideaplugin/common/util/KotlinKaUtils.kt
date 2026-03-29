@file:OptIn(KaContextParameterApi::class)

package dev.slne.surf.idea.surfideaplugin.common.util

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.defaultType
import org.jetbrains.kotlin.analysis.api.components.isSubClassOf
import org.jetbrains.kotlin.analysis.api.symbols.classSymbol
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
fun KtClass.hasAnnotation(annotationId: ClassId): Boolean {
    return symbol.annotations.any { it.classId == annotationId }
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