package dev.slne.surf.idea.surfideaplugin.redis.toolwindow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import dev.slne.surf.idea.surfideaplugin.redis.SurfRedisClassNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys

data class PsiEntityInfo(
    val name: String,
    val qualifiedName: String,
    val element: PsiElement
)

data class SurfRedisProjectData(
    val events: List<PsiEntityInfo> = emptyList(),
    val requests: List<PsiEntityInfo> = emptyList(),
    val responses: List<PsiEntityInfo> = emptyList(),
    val eventHandlers: List<PsiEntityInfo> = emptyList(),
    val requestHandlers: List<PsiEntityInfo> = emptyList(),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SurfRedisOverviewTab(project: Project) {
    var data by remember { mutableStateOf(SurfRedisProjectData()) }
    var isLoading by remember { mutableStateOf(true) }


    LaunchedEffect(isLoading) {
        if (!isLoading) return@LaunchedEffect
        data = withContext(Dispatchers.Default) {
            readAction { scanProject(project) }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Tooltip(tooltip = { Text("Refresh all surf-redis entities") }) {
                IconButton(onClick = { isLoading = true }) {
                    Icon(
                        key = AllIconsKeys.Actions.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isLoading) {
                Text(
                    "Scanning...",
                    fontSize = 12.sp,
                    color = JewelTheme.globalColors.text.info
                )
            }
        }

        // Content
        if (!isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                categorySection(
                    title = "Events",
                    icon = AllIconsKeys.Nodes.Class,
                    items = data.events
                )

                categorySection(
                    title = "Requests",
                    icon = AllIconsKeys.Nodes.Interface,
                    items = data.requests
                )

                categorySection(
                    title = "Responses",
                    icon = AllIconsKeys.Nodes.AbstractClass,
                    items = data.responses
                )

                categorySection(
                    title = "Event Handlers",
                    icon = AllIconsKeys.Nodes.Method,
                    items = data.eventHandlers
                )

                categorySection(
                    title = "Request Handlers",
                    icon = AllIconsKeys.Nodes.Method,
                    items = data.requestHandlers
                )
            }
        }
    }
}

private fun LazyListScope.categorySection(
    title: String,
    icon: IconKey,
    items: List<PsiEntityInfo>
) {
    item(key = "header_$title") {
        CollapsibleCategoryHeader(
            title = title,
            count = items.size,
            icon = icon,
            items = items
        )
    }
}

@Composable
private fun CollapsibleCategoryHeader(
    title: String,
    count: Int,
    icon: IconKey,
    items: List<PsiEntityInfo>
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                key = if (expanded)
                    AllIconsKeys.General.ArrowDown
                else
                    AllIconsKeys.General.ArrowRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(12.dp)
            )

            Spacer(Modifier.width(4.dp))

            Icon(
                key = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "$title ($count)",
                fontSize = 13.sp
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                items.sortedBy { it.name }.forEach { entity ->
                    EntityItem(entity, icon)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntityItem(
    entity: PsiEntityInfo,
    categoryIcon: IconKey
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Tooltip(tooltip = {
        Text(entity.qualifiedName.ifEmpty { entity.name })
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .then(
                    if (isHovered) Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JewelTheme.globalColors.toolwindowBackground)
                    else Modifier
                )
                .clickable {
                    val navigatable = entity.element as? Navigatable
                    if (navigatable?.canNavigate() == true) {
                        navigatable.navigate(true)
                    }
                }
                .padding(start = 28.dp, top = 3.dp, bottom = 3.dp, end = 8.dp)
        ) {
            Icon(
                key = categoryIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = entity.name,
                fontSize = 13.sp
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

private fun scanProject(project: Project): SurfRedisProjectData {
    val scope = GlobalSearchScope.projectScope(project)
    val facade = JavaPsiFacade.getInstance(project)

    return SurfRedisProjectData(
        events = findSubclasses(facade, project, SurfRedisClassNames.REDIS_EVENT_CLASS, scope),
        requests = findSubclasses(facade, project, SurfRedisClassNames.REDIS_REQUEST_CLASS, scope),
        responses = findSubclasses(facade, project, SurfRedisClassNames.REDIS_RESPONSE_CLASS, scope),
        eventHandlers = findAnnotatedMethods(facade, project, SurfRedisClassNames.ON_REDIS_EVENT_ANNOTATION, scope),
        requestHandlers = findAnnotatedMethods(
            facade,
            project,
            SurfRedisClassNames.HANDLE_REDIS_REQUEST_ANNOTATION,
            scope
        ),
    )
}

private fun findSubclasses(
    facade: JavaPsiFacade, project: Project, baseFqn: String, scope: GlobalSearchScope
): List<PsiEntityInfo> {
    val baseClass = facade.findClass(baseFqn, GlobalSearchScope.allScope(project)) ?: return emptyList()
    return ClassInheritorsSearch.search(baseClass, scope, true).mapNotNull { psiClass ->
        PsiEntityInfo(
            name = psiClass.name ?: return@mapNotNull null,
            qualifiedName = psiClass.qualifiedName ?: "",
            element = psiClass.navigationElement
        )
    }
}

private fun findAnnotatedMethods(
    facade: JavaPsiFacade, project: Project, annotationFqn: String, scope: GlobalSearchScope
): List<PsiEntityInfo> {
    val annotationClass = facade.findClass(annotationFqn, GlobalSearchScope.allScope(project)) ?: return emptyList()
    return AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope).mapNotNull { method ->
        val containingClass = method.containingClass?.name ?: "?"
        PsiEntityInfo(
            name = "$containingClass.${method.name}()",
            qualifiedName = "${method.containingClass?.qualifiedName}.${method.name}",
            element = method.navigationElement
        )
    }
}