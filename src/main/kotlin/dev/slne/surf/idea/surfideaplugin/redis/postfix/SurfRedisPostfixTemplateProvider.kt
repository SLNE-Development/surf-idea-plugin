package dev.slne.surf.idea.surfideaplugin.redis.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class SurfRedisPostfixTemplateProvider: PostfixTemplateProvider {
    private val templateSet: Set<PostfixTemplate> by lazy {
        setOf(
            RedisPublishPostfixTemplate(this),
            RedisSendRequestPostfixTemplate(this),
        )
    }

    override fun getTemplates(): Set<PostfixTemplate> = templateSet

    override fun isTerminalSymbol(currentChar: Char): Boolean = currentChar == '.'

    override fun preExpand(file: PsiFile, editor: Editor) {}
    override fun afterExpand(file: PsiFile, editor: Editor) {}
    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile
}