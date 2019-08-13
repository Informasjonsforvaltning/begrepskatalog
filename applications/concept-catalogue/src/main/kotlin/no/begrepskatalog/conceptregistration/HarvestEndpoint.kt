package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.api.CollectionsApi
import no.begrepskatalog.generated.model.Begrep
import no.fdk.concept.builder.ModelBuilder
import no.fdk.concept.builder.SKOSNO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest


@RestController
@CrossOrigin(value = "*")
class HarvestEndpoint(val sqlStore: SqlStore) : CollectionsApi {

    @Value("\${application.baseURL}")
    lateinit var baseURL: String

    private val logger = LoggerFactory.getLogger(HarvestEndpoint::class.java)

    override fun getCollections(httpServletRequest: HttpServletRequest, publisher: String): ResponseEntity<Any> {
        logger.info("Harvest - request")

        val allPublishedBegrep = sqlStore.getAllPublishedBegrep()

        logger.info("Harvest found ${allPublishedBegrep.size} Begrep")

        val allConvertedBegrep = mutableListOf<Resource>()

        val modelBuilder = ModelBuilder.builder()

        for (begrep in allPublishedBegrep) {
            val resource = convertBegrepIntoModel(begrep, modelBuilder)
            allConvertedBegrep.add(resource)
            modelBuilder.collectionBuilder("")
                    .publisher(begrep.ansvarligVirksomhet.id)
                    .member(resource)
        }

        var collectionModel: Model = modelBuilder.collectionBuilder("https://registrering-begrep.ut1.fellesdatakatalog.brreg.no/").build()
        val writer = StringWriter()
        collectionModel.write(writer, "TURTLE")
        println(writer.buffer)

        return ResponseEntity.ok(writer.buffer.toString())
    }

    fun convertBegrepIntoModel(begrep: Begrep, modelBuilder: ModelBuilder): Resource {
        val sourceReference = if (begrep.kildebeskrivelse != null) begrep.kildebeskrivelse.forholdTilKilde.toString() else ""
        var sourceItself = if (begrep.kildebeskrivelse != null && begrep.kildebeskrivelse.kilde != null && begrep.kildebeskrivelse.kilde.size > 0) begrep.kildebeskrivelse.kilde[0].uri + begrep.kildebeskrivelse.kilde[0].tekst else ""
        val merknad = if (begrep.merknad != null) begrep.merknad else ""
        val anbefaltTerm = if (begrep.anbefaltTerm != null) begrep.anbefaltTerm else ""
        val urlForAccessingThisBegrepsRegistration = baseURL + begrep.ansvarligVirksomhet.id + "/" + begrep.id
        val resource = modelBuilder.conceptBuilder(urlForAccessingThisBegrepsRegistration)
                .publisher(begrep.ansvarligVirksomhet.id)
                .definitionBuilder(SKOSNO.Definisjon)
                .text(begrep.definisjon, "nb")
                .source(sourceItself, "nb", sourceReference)
                .audience("allmenheten", "nb")
                .scopeNote(merknad, "nb")
                .modified(begrep.gyldigFom.toString())
                .build()
                .identifier(begrep.id)
                .preferredTerm(anbefaltTerm, "no")
                .resource
        return resource
    }
}