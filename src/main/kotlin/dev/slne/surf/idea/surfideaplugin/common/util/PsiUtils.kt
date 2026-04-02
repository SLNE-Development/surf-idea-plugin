package dev.slne.surf.idea.surfideaplugin.common.util

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass

fun PsiClass.isConcreteClass(): Boolean {
    return !isInterface
            && !isEnum
            && !isAnnotationType
            && !hasModifierProperty(PsiModifier.ABSTRACT)
}

suspend fun KtLightClass.getDestructuringParamNames(): List<String>? {
    val ktClass = kotlinOrigin as? KtClass ?: return null
    return readAction(fun(): List<String>? {
        if (!ktClass.isData()) return null

        return analyze(ktClass) {
            val classSymbol = ktClass.namedClassSymbol ?: return null
            val primaryConstructor = classSymbol.memberScope.constructors.find { it.isPrimary } ?: return null

            val params = primaryConstructor.valueParameters
            if (params.isEmpty()) return null

            params.map { it.name.asString() }
        }
    })
}

fun KtAnnotated.hasAnnotationPsi(fqName: FqName): Boolean {
    return KotlinPsiHeuristics.hasAnnotation(this, fqName)
}