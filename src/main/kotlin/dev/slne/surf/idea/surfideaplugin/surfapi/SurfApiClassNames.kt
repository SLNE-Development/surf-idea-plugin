package dev.slne.surf.idea.surfideaplugin.surfapi

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfApiClassNames {
    const val INTERNAL_API_MARKER_ANNOTATION = "dev.slne.surf.api.shared.api.annotation.InternalAPIMarker"

    val INTERNAL_API_MARKER_ANNOTATION_FQN = FqName(INTERNAL_API_MARKER_ANNOTATION)

    val INTERNAL_API_MARKER_ANNOTATION_ID = ClassId.topLevel(INTERNAL_API_MARKER_ANNOTATION_FQN)
}