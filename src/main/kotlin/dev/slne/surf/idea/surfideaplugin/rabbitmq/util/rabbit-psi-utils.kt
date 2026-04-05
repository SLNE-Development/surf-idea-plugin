package dev.slne.surf.idea.surfideaplugin.rabbitmq.util

import dev.slne.surf.idea.surfideaplugin.common.util.hasAnnotationPsi
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import org.jetbrains.kotlin.psi.KtAnnotated

fun KtAnnotated.isRabbitHandlerPsi() = hasAnnotationPsi(SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION_FQN)