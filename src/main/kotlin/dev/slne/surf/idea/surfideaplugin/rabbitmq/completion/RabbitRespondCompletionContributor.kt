package dev.slne.surf.idea.surfideaplugin.rabbitmq.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ProcessingContext
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.util.RabbitRespondTypeResolver
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

class RabbitRespondCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(KotlinLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val parent = position.parent
                    val parent1 = parent?.parent
                    val dotExpression = parent1 as? KtDotQualifiedExpression ?: return
                    val receiverExpression = dotExpression.receiverExpression

                    val responseClassId = analyze(receiverExpression) {
                        val receiverType = receiverExpression.expressionType ?: return@analyze null
                        if (!receiverType.isSubtypeOf(SurfRabbitClassNames.RABBIT_REQUEST_PACKET_CLASS_ID)) return@analyze null
                        RabbitRespondTypeResolver.resolveResponseTypeName(receiverType)
                    } ?: return

                    val lookupElement = LookupElementBuilder
                        .create("respond")
                        .withTailText("(${responseClassId.shortClassName.asString()}())")
                        .withTypeText("surf-rabbit")
                        .withIcon(AllIcons.Providers.RabbitMQ)
                        .withInsertHandler(fun(insertContext, element) {
                            val editor = insertContext.editor
                            val document = insertContext.document
                            val project = insertContext.project
                            val responseTypeFqn = responseClassId.asFqNameString()

                            val tailOffset = insertContext.tailOffset
                            val insertText = "($responseTypeFqn())"
                            document.insertString(tailOffset, insertText)

                            val cursorOffset = tailOffset + "($responseTypeFqn(".length
                            editor.caretModel.moveToOffset(cursorOffset)

                            PsiDocumentManager.getInstance(project).commitDocument(document)

                            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return
                            val ktFile = psiFile as? KtFile ?: return

                            val replaceStart = insertContext.startOffset
                            val replaceEnd = tailOffset + insertText.length
                            ShortenReferencesFacility.getInstance()
                                .shorten(ktFile, TextRange.create(replaceStart, replaceEnd))

                            runInEdt {
                                AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
                            }
                        })
                        .bold()

                    result.addElement(
                        PrioritizedLookupElement.withPriority(lookupElement, Double.MAX_VALUE)
                    )
                }
            }
        )
    }
}