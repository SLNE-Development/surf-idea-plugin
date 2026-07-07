package dev.slne.surf.idea.surfideaplugin.rabbitmq.icons

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.SurfStandardIcons
import dev.slne.surf.idea.surfideaplugin.common.library.SurfLibraryMarker
import dev.slne.surf.idea.surfideaplugin.common.library.hasLibrary
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.isRpcServicePsi
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.Icon

class RpcServiceIconProvider : KotlinIconProvider() {

    override fun isMatchingExpected(declaration: KtDeclaration): Boolean = false

    override fun getIcon(psiElement: PsiElement, flags: Int): Icon? {
        val rpcInterface = when (psiElement) {
            is KtClass -> psiElement
            is KtFile -> psiElement.declarations.singleOrNull() as? KtClass ?: return null
            else -> return null
        }

        if (!rpcInterface.isInterface()) return null
        if (!rpcInterface.isRpcServicePsi()) return null
        if (!hasRabbitLibrary(rpcInterface)) return null

        return SurfStandardIcons.RpcService
    }

    private fun hasRabbitLibrary(element: PsiElement): Boolean {
        if (DumbService.isDumb(element.project)) return true

        val module = element.module ?: return false
        return module.hasLibrary(SurfLibraryMarker.SURF_RABBITMQ_COMMON_API)
    }
}
