package dev.slne.surf.idea.surfideaplugin.rabbitmq.util

import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotation
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.psi.KtNamedFunction

context(_: KaSession)
fun KtNamedFunction.isRabbitHandler() = hasAnnotation(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_ID)