package dev.slne.surf.idea.surfideaplugin.util

import com.intellij.psi.PsiClass
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import dev.slne.surf.idea.surfideaplugin.common.SurfCommonClassNames

fun PsiClass.className(): ClassName? {
    return qualifiedName?.let { ClassName.bestGuess(it) }
}

fun className(
    packageName: String,
    simpleName: String,
    fqName: String?
): ClassName {
    if (!fqName.isNullOrBlank()) {
        return ClassName.bestGuess(fqName)
    }

    return if (packageName.isBlank()) {
        ClassName("", simpleName)
    } else {
        ClassName(packageName, simpleName)
    }
}

fun serializableAnnotationSpec(): AnnotationSpec {
    return AnnotationSpec.builder(SurfCommonClassNames.KOTLINX_SERIALIZABLE_ANNOTATION_CLASS_NAME).build()
}