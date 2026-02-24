@file:OptIn(KaContextParameterApi::class)

package dev.slne.surf.idea.surfideaplugin.common.util

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

context(_: KaSession)
fun KtNamedFunction.hasAnnotation(annotation: ClassId): Boolean {
    return symbol.annotations.any { it.classId == annotation }
}

context(_: KaSession)
fun KtNamedFunction.findValueParameter(annotation: ClassId): KtParameter? {
    for (parameter in valueParameters) {
        val type = parameter.symbol.returnType as? KaClassType ?: continue
        if (type.classId == annotation) return parameter
    }

    return null
}