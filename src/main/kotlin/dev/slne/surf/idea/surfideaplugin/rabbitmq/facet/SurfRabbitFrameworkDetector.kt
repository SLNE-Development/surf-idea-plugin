package dev.slne.surf.idea.surfideaplugin.rabbitmq.facet

import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.patterns.ElementPattern
import com.intellij.util.indexing.FileContent

class SurfRabbitFrameworkDetector :
    FacetBasedFrameworkDetector<SurfRabbitFacet, SurfRabbitFacetConfiguration>("surf-rabbit") {
    override fun getFacetType() = SurfRabbitFacetType()

    override fun getFileType(): FileType {
        return FileTypeManager.getInstance().getFileTypeByExtension("kt")
    }

    override fun createSuitableFilePattern(): ElementPattern<FileContent> = FileContentPattern.fileContent()
}