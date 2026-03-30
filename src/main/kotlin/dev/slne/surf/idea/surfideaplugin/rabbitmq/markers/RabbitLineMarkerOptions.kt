package dev.slne.surf.idea.surfideaplugin.rabbitmq.markers

import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.icons.AllIcons
import dev.slne.surf.idea.surfideaplugin.SurfStandardIcons

object RabbitLineMarkerOptions {
    val requestHandlerOption = GutterIconDescriptor.Option(
        "surf.rabbit.request-handler",
        "Rabbit request handler",
        AllIcons.Providers.RabbitMQ
    )
    val requestCallOption = GutterIconDescriptor.Option(
        "surf.rabbit.request-call",
        "Rabbit request call",
        SurfStandardIcons.Publisher
    )
}