package dev.slne.surf.idea.surfideaplugin.redis.postfix

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import org.jetbrains.kotlin.idea.core.unblockDocument

class RedisSendRequestPostfixTemplate(provider: SurfRedisPostfixTemplateProvider) :
    PostfixTemplateWithExpressionSelector(
        /* id = */ "surf.redis.send",
        /* name = */ "send",
        /* example = */ "redisApi.sendRequest<ResponseType>(expr)",
        /* selector = */ redisExpressionSelector(SurfRedisClassNames.REDIS_REQUEST_CLASS),
        /* provider = */ provider
    ) {

    override fun expandForChooseExpression(expression: PsiElement, editor: Editor) {
        val project = expression.project
        val expressionText = expression.text

        expression.delete()

        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        template.isToReformat = true

        template.addVariable("method", TextExpression("redisApi.sendRequest"), true)
        template.addTextSegment("<")
        template.addVariable("responseType", TextExpression("TODO()"), true)
        template.addTextSegment(">(")
        template.addTextSegment(expressionText)
        template.addTextSegment(")")
        template.addEndVariable()
        template.addTextSegment(System.lineSeparator())

        editor.unblockDocument()
        templateManager.startTemplate(editor, template)
    }
}