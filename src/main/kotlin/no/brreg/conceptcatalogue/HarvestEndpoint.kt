package no.brreg.conceptcatalogue

import no.begrepskatalog.generated.api.CollectionsApi
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Kildebeskrivelse
import no.begrepskatalog.generated.model.Status
import no.brreg.conceptcatalogue.repository.BegrepRepository
import no.difi.skos_ap_no.concept.builder.Conceptcollection.CollectionBuilder
import no.difi.skos_ap_no.concept.builder.ModelBuilder
import no.difi.skos_ap_no.concept.builder.generic.SourceType
import org.apache.jena.rdf.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest

@RestController
class HarvestEndpoint(val begrepRepository: BegrepRepository, val mongoOperations: MongoOperations) : CollectionsApi {

    @Value("\${application.baseURL}")
    lateinit var baseURL: String

    private val logger = LoggerFactory.getLogger(HarvestEndpoint::class.java)

    override fun getCollections(httpServletRequest: HttpServletRequest, publisher: String?): ResponseEntity<Any> {
        logger.info("Harvest - request")

        val modelBuilder = ModelBuilder.builder()

        if (publisher == null) {
            getCollections(httpServletRequest, modelBuilder);
        } else {
            getCollection(httpServletRequest, modelBuilder, publisher);
        }

        val writer = StringWriter()
        // TODO: add test for Model object
        modelBuilder.build().write(writer, "TURTLE")

        return ResponseEntity.ok(writer.buffer.toString())
    }

    fun getCollections(httpServletRequest: HttpServletRequest, modelBuilder: ModelBuilder) {
        val publisherIds = mongoOperations.query(Begrep::class.java).distinct("ansvarligVirksomhet.id").`as`(String::class.java).all()

        for (publisherId in publisherIds) {
            getCollection(httpServletRequest, modelBuilder, publisherId)
        }
    }

    fun getCollection(httpServletRequest: HttpServletRequest, modelBuilder: ModelBuilder, publisherId: String) {

        val allPublishedBegrepByCompany = begrepRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(publisherId, Status.PUBLISERT)

        logger.info("Harvest $publisherId found ${allPublishedBegrepByCompany.size} Begrep")

        val collectionBuilder = modelBuilder
                .collectionBuilder("https://registrering-begrep.fellesdatakatalog.brreg.no/$publisherId")
                .publisher(publisherId)
                .name("$publisherId sin samling")

        for (begrep in allPublishedBegrepByCompany) {
            appendBegrepToCollection(begrep, collectionBuilder)
        }

        collectionBuilder.build()
    }

    fun appendBegrepToCollection(begrep: Begrep, collectionBuilder: CollectionBuilder): Resource {
        val urlForAccessingThisBegrepsRegistration = baseURL + begrep.ansvarligVirksomhet.id + "/" + begrep.id

        var conceptBuilder = collectionBuilder.conceptBuilder(urlForAccessingThisBegrepsRegistration)
        var definitionBuilder = conceptBuilder.definitionBuilder()
        val sourceDescriptionBuilder = definitionBuilder.sourcedescriptionBuilder()
        val sourceBuilder = sourceDescriptionBuilder.sourceBuilder()


        if (begrep.kildebeskrivelse != null) {

            when (begrep.kildebeskrivelse.forholdTilKilde) {
                Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT -> sourceDescriptionBuilder.sourcetype(SourceType.Source.Userdefined)
                Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE -> sourceDescriptionBuilder.sourcetype(SourceType.Source.BasedOn)
                Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE -> sourceDescriptionBuilder.sourcetype(SourceType.Source.QuoteFrom)
            }
            begrep.kildebeskrivelse.kilde?.forEach {
                sourceBuilder.label(it.tekst, "nb")
                        .seeAlso(it.uri)
            }

            sourceBuilder.build()
            sourceDescriptionBuilder.build()
        }

        begrep.definisjon.tekst.forEach {
            handleMultilingualFieldEntry(it) { definisjon: String -> definitionBuilder = definitionBuilder.text(definisjon, it.key) }
        }

        begrep.merknad?.forEach {
            handleMultilingualFieldEntry(it) { merknad: String -> definitionBuilder = definitionBuilder.scopeNote(merknad, it.key) }
        }

        conceptBuilder = definitionBuilder.scopeBuilder()
                .label(begrep.omfang?.tekst ?: "", "nb")
                .seeAlso(begrep.omfang?.uri)
                .build()
                .modified(begrep.gyldigFom)
                .build()

        var prefLabelBuilder = conceptBuilder
                .identifier(begrep.id)
                .publisher(begrep.ansvarligVirksomhet.id)
                .prefLabelBuilder()

        begrep.anbefaltTerm.navn.forEach {
            handleMultilingualFieldEntry(it) { anbefaltTerm: String -> prefLabelBuilder = prefLabelBuilder.label(anbefaltTerm, it.key) }
        }

        conceptBuilder = prefLabelBuilder.build()

        begrep.eksempel?.forEach {
            handleMultilingualFieldEntry(it) { eksempel: String -> conceptBuilder = conceptBuilder.example(eksempel, it.key) }
        }

        begrep.fagområde?.forEach {
            handleMultilingualFieldEntry(it) { fagområde: String -> conceptBuilder = conceptBuilder.subject(fagområde, it.key) }
        }

        conceptBuilder = conceptBuilder.contactPointBuilder()
                .email(begrep.kontaktpunkt?.harEpost ?: "")
                .telephone(begrep.kontaktpunkt?.harTelefon ?: "")
                .build()

        begrep.bruksområde?.forEach {
            handleMultilingualFieldArrayEntry(it) { bruksområde: String -> conceptBuilder = conceptBuilder.domainOfUse(bruksområde, it.key) }
        }

        var altLabelBuilder = conceptBuilder.altLabelBuilder()
        begrep.tillattTerm?.forEach {
            handleMultilingualFieldArrayEntry(it) { tillattTerm: String -> altLabelBuilder = altLabelBuilder.label(tillattTerm, it.key) }
        }
        conceptBuilder = altLabelBuilder.build()

        var hiddenLabelBuilder = conceptBuilder.hiddenLabelBuilder()
        begrep.frarådetTerm?.forEach {
            handleMultilingualFieldArrayEntry(it) { frarådetTerm: String -> hiddenLabelBuilder = hiddenLabelBuilder.label(frarådetTerm, it.key) }
        }
        conceptBuilder = hiddenLabelBuilder.build()

        return conceptBuilder.build().resource
    }

    private fun handleMultilingualFieldEntry(entry: Map.Entry<String, Any>, callback: (value: String) -> Unit): Unit = when {
        entry.value is String -> callback(entry.value as String)
        else -> {
        }
    }

    private fun handleMultilingualFieldArrayEntry(entry: Map.Entry<String, Any>, callback: (value: String) -> Unit): Unit = when {
        entry.value is List<*> -> (entry.value as List<*>).forEach { callback(it as String) }
        else -> {
        }
    }
}
