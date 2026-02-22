package dev.slne.surf.idea.surfideaplugin.redis.facet

import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.patterns.ElementPattern
import com.intellij.util.indexing.FileContent

class SurfRedisFrameworkDetector :
    FacetBasedFrameworkDetector<SurfRedisFacet, SurfRedisFacetConfiguration>("surf-redis") {
    override fun getFacetType() = SurfRedisFacetType()

    override fun getFileType(): FileType {
        return FileTypeManager.getInstance().getFileTypeByExtension("kt")
    }

    override fun createSuitableFilePattern(): ElementPattern<FileContent> = FileContentPattern.fileContent()
}