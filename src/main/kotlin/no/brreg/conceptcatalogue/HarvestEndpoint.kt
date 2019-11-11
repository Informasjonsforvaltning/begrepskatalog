package no.brreg.conceptcatalogue

import no.begrepskatalog.generated.api.CollectionsApi
import no.brreg.conceptcatalogue.service.SkosApNoModelService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class HarvestEndpoint(
        private val skosApNoModelService: SkosApNoModelService
) : CollectionsApi {
    private val logger = LoggerFactory.getLogger(HarvestEndpoint::class.java)

    override fun getCollections(httpServletRequest: HttpServletRequest, publisher: String?): ResponseEntity<Any> {
        return ResponseEntity.ok(skosApNoModelService.serializeAsTextTurtle(when {
            publisher is String && !publisher.isNullOrBlank() -> skosApNoModelService.buildModelForPublishersCollection(publisher)
            else -> skosApNoModelService.buildModelForAllCollections()
        }))
    }
}
