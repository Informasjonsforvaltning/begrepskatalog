package no.brreg.conceptcatalogue.repository

import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface BegrepRepository : MongoRepository<Begrep, String> {

    fun getBegrepByAnsvarligVirksomhetId(orgNr: String): List<Begrep>

    fun getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: Status): List<Begrep>

    fun getBegrepById(id: String): Begrep?

    fun removeBegrepById(id: String): Long
}
