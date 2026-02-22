package dev.slne.surf.idea.surfideaplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

private const val BUNDLE = "messages.SurfBundle"

internal object SurfBundle {
    private val instance = DynamicBundle(SurfBundle::class.java, BUNDLE)

    @JvmStatic
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
        return instance.getMessage(key, *params)
    }

    @JvmStatic
    @Nls
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): Supplier<String> {
        return instance.getLazyMessage(key, *params)
    }
}