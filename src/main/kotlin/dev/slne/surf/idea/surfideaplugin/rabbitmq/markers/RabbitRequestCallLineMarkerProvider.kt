package dev.slne.surf.idea.surfideaplugin.rabbitmq.markers

import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.DumbService
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.InheritanceUtil
import dev.slne.surf.idea.surfideaplugin.SurfStandardIcons
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitClassNames
import dev.slne.surf.idea.surfideaplugin.rabbitmq.SurfRabbitConstants
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtCallExpression
import javax.swing.Icon

class RabbitRequestCallLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String = "Rabbit Request call handler navigation"
    override fun getIcon(): Icon = SurfStandardIcons.Publisher

    override fun getLineMarkerInfo(p0: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        if (elements.isEmpty()) return
        if (!RabbitLineMarkerOptions.requestCallOption.isEnabled) return

        val first = elements.first()
        if (DumbService.isDumb(first.project)) return

        for (element in elements) {
            val ktCallExpression = element as? KtCallExpression ?: continue
            val module = ktCallExpression.module ?: continue

            val requestClassFqn = analyze(ktCallExpression) {
                val callInfo = ktCallExpression.resolveToCall() ?: return@analyze null
                val successCall = callInfo.singleFunctionCallOrNull() ?: return@analyze null
                val symbol = successCall.symbol

                val callableId = symbol.callableId ?: return@analyze null
                val classId = callableId.classId

                if (classId != SurfRabbitClassNames.CLIENT_RABBIT_MQ_API_CLASS_ID ||
                    callableId.callableName.asString() != SurfRabbitConstants.RABBIT_REQUEST_METHOD_NAME
                ) {
                    return@analyze null
                }

                val firstArg = ktCallExpression.valueArguments.firstOrNull()
                    ?.getArgumentExpression() ?: return@analyze null

                val argType = firstArg.expressionType as? KaClassType ?: return@analyze null
                argType.classId
            } ?: continue

            val project = ktCallExpression.project
            val psiFacade = JavaPsiFacade.getInstance(project)

            val requestClass = psiFacade.findClass(
                requestClassFqn.asFqNameString(),
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
            ) ?: continue

            val annotationClass = psiFacade.findClass(
                SurfRabbitClassNames.RABBIT_HANDLER_ANNOTATION,
                GlobalSearchScope.allScope(project)
            ) ?: continue

            val handlers = findMatchingHandlers(annotationClass, requestClassFqn.asFqNameString(), requestClass)
            if (handlers.isEmpty()) continue

            val builder = NavigationGutterIconBuilder.create(icon)
                .setTargets(handlers)
                .setTooltipText("Go to @RabbitHandler handlers")
                .setPopupTitle("@RabbitHandler Handlers for ${requestClass.name}")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun findMatchingHandlers(
        annotationClass: PsiClass,
        requestClassFqn: String,
        requestClass: PsiClass
    ): List<PsiElement> {
        val handlers = mutableListOf<PsiElement>()

        AnnotatedElementsSearch.searchPsiMethods(
            annotationClass,
            GlobalSearchScope.allScope(annotationClass.project)
        ).forEach { method ->
            val params = method.parameterList.parameters
            if (params.size < 1) return@forEach

            val paramType = params[0].type
            if (matchesRequestType(paramType, requestClassFqn, requestClass)) {
                handlers.add(method.navigationElement)
            }
        }

        return handlers
    }

    private fun matchesRequestType(
        paramType: PsiType,
        requestClassFqn: String,
        requestClass: PsiClass
    ): Boolean {
        val paramClass = (paramType as? PsiClassType)?.resolve() ?: return false
        return paramClass.qualifiedName == requestClassFqn || InheritanceUtil.isInheritorOrSelf(
            requestClass,
            paramClass,
            true
        )
    }
}