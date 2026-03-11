package dev.slne.surf.idea.surfideaplugin.surfapi.paper.util

enum class PaperEventListenerPriorities {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR;

    companion object {
        val names = entries.map { it.name }
    }
}