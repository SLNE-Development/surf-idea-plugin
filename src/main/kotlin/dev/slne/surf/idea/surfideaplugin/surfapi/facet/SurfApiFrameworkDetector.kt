package dev.slne.surf.idea.surfideaplugin.surfapi.facet

import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.patterns.ElementPattern
import com.intellij.util.indexing.FileContent
import dev.slne.surf.idea.surfideaplugin.common.facet.SurfLibraryDetector
import dev.slne.surf.idea.surfideaplugin.surfapi.platform.SurfApiPlatform
import java.util.*

class SurfApiFrameworkDetector : FacetBasedFrameworkDetector<SurfApiFacet, SurfApiFacetConfiguration>("surf-api") {
    override fun getFacetType() = SurfApiFacetType()

    override fun getFileType(): FileType {
        return FileTypeManager.getInstance().getFileTypeByExtension("kt")
    }

    override fun createSuitableFilePattern(): ElementPattern<FileContent> = FileContentPattern.fileContent()

    override fun setupFacet(facet: SurfApiFacet, model: ModifiableRootModel) {
        val module = facet.module
        val platforms = EnumSet.of(SurfApiPlatform.CORE)

        if (SurfLibraryDetector.hasSurfApiPaper(module)) {
            platforms.add(SurfApiPlatform.PAPER)
        }

        if (SurfLibraryDetector.hasSurfApiVelocity(module)) {
            platforms.add(SurfApiPlatform.VELOCITY)
        }

        facet.configuration.detectedPlatforms = platforms
    }
}