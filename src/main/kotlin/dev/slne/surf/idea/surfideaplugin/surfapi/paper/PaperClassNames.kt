package dev.slne.surf.idea.surfideaplugin.surfapi.paper

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object PaperClassNames {
    const val EVENT_CLASS = "org.bukkit.event.Event"
    const val LISTENER_CLASS = "org.bukkit.event.Listener"
    const val EVENT_HANDLER_ANNOTATION = "org.bukkit.event.EventHandler"
    const val EVENT_PRIORITY_CLASS = "org.bukkit.event.EventPriority"
    const val HANDLER_LIST_CLASS = "org.bukkit.event.HandlerList"

    val LISTENER_CLASS_FQN = FqName(LISTENER_CLASS)
    val EVENT_HANDLER_ANNOTATION_FQN = FqName(EVENT_HANDLER_ANNOTATION)

    val LISTENER_CLASS_ID = ClassId.topLevel(LISTENER_CLASS_FQN)

    val EVENT_HANDLER_ANNOTATION_CLASS_NAME = ClassName.bestGuess(EVENT_HANDLER_ANNOTATION)
    val EVENT_PRIORITY_CLASS_NAME = ClassName.bestGuess(EVENT_PRIORITY_CLASS)
}