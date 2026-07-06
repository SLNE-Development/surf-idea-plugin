package dev.slne.surf.idea.surfideaplugin.surfapi.velocity

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.name.FqName

object VelocityClassNames {
    const val SUBSCRIBE_ANNOTATION = "com.velocitypowered.api.event.Subscribe"

    val SUBSCRIBE_ANNOTATION_FQN = FqName(SUBSCRIBE_ANNOTATION)

    val SUBSCRIBE_ANNOTATION_CLASS_NAME = ClassName.bestGuess(SUBSCRIBE_ANNOTATION)
}