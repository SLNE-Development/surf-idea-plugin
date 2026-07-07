package dev.slne.surf.idea.surfideaplugin.surfapi

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SurfApiClassNames {
    const val INTERNAL_API_MARKER_ANNOTATION = "dev.slne.surf.api.shared.api.annotation.InternalAPIMarker"

    const val SURF_EVENT_HANDLER_ANNOTATION = "dev.slne.surf.api.core.event.SurfEventHandler"
    const val SURF_EVENT_CLASS = "dev.slne.surf.api.core.event.SurfEvent"
    const val SURF_SYNC_EVENT_CLASS = "dev.slne.surf.api.core.event.SurfSyncEvent"
    const val SURF_ASYNC_EVENT_CLASS = "dev.slne.surf.api.core.event.SurfAsyncEvent"

    const val ABSTRACT_COMPONENT_CLASS = "dev.slne.surf.api.core.component.AbstractComponent"
    const val SURF_COMPONENT_META_ANNOTATION = "dev.slne.surf.api.shared.api.component.SurfComponentMeta"

    val INTERNAL_API_MARKER_ANNOTATION_FQN = FqName(INTERNAL_API_MARKER_ANNOTATION)
    val SURF_EVENT_HANDLER_ANNOTATION_FQN = FqName(SURF_EVENT_HANDLER_ANNOTATION)
    val SURF_EVENT_CLASS_FQN = FqName(SURF_EVENT_CLASS)
    val SURF_SYNC_EVENT_CLASS_FQN = FqName(SURF_SYNC_EVENT_CLASS)
    val SURF_ASYNC_EVENT_CLASS_FQN = FqName(SURF_ASYNC_EVENT_CLASS)
    val ABSTRACT_COMPONENT_CLASS_FQN = FqName(ABSTRACT_COMPONENT_CLASS)
    val SURF_COMPONENT_META_ANNOTATION_FQN = FqName(SURF_COMPONENT_META_ANNOTATION)

    val INTERNAL_API_MARKER_ANNOTATION_ID = ClassId.topLevel(INTERNAL_API_MARKER_ANNOTATION_FQN)
    val SURF_EVENT_HANDLER_ANNOTATION_ID = ClassId.topLevel(SURF_EVENT_HANDLER_ANNOTATION_FQN)
    val SURF_EVENT_CLASS_ID = ClassId.topLevel(SURF_EVENT_CLASS_FQN)
    val SURF_SYNC_EVENT_CLASS_ID = ClassId.topLevel(SURF_SYNC_EVENT_CLASS_FQN)
    val SURF_ASYNC_EVENT_CLASS_ID = ClassId.topLevel(SURF_ASYNC_EVENT_CLASS_FQN)
    val ABSTRACT_COMPONENT_CLASS_ID = ClassId.topLevel(ABSTRACT_COMPONENT_CLASS_FQN)
    val SURF_COMPONENT_META_ANNOTATION_ID = ClassId.topLevel(SURF_COMPONENT_META_ANNOTATION_FQN)
}