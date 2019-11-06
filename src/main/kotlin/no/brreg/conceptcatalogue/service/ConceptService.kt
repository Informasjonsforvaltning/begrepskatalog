package no.brreg.conceptcatalogue.service

import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.brreg.conceptcatalogue.repository.BegrepRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.stereotype.Service

@Service
class ConceptService(
        private val begrepRepository: BegrepRepository,
        private val mongoOperations: MongoOperations
) {
    private val logger = LoggerFactory.getLogger(ConceptService::class.java)

    fun getAllPublisherIds(): List<String> {
        logger.info("Retrieving publisher IDs")
        return mongoOperations
                .query(Begrep::class.java)
                .distinct("ansvarligVirksomhet.id")
                .`as`(String::class.java)
                .all()
    }

    fun getPublishedConceptsForPublisherId(publisherId: String): List<Begrep> {
        logger.info("Retrieving all published concepts for publisher: {}", publisherId)
        return getConceptsForPublisherIdAndStatus(publisherId, Status.PUBLISERT)
    }

    private fun getConceptsForPublisherIdAndStatus(publisherId: String, status: Status): List<Begrep> {
        logger.info("Retrieving all concepts with status: {} for publisher: {}", status, publisherId)
        return begrepRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(publisherId, status)
    }
}