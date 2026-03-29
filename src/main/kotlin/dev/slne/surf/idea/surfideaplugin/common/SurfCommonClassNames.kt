package dev.slne.surf.idea.surfideaplugin.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfCommonClassNames {

    const val KOTLINX_SERIALIZABLE_ANNOTATION = "kotlinx.serialization.Serializable"

    val KOTLINX_SERIALIZABLE_ANNOTATION_FQN = FqName(KOTLINX_SERIALIZABLE_ANNOTATION)

    val KOTLINX_SERIALIZABLE_ANNOTATION_ID = ClassId.topLevel(KOTLINX_SERIALIZABLE_ANNOTATION_FQN)
}