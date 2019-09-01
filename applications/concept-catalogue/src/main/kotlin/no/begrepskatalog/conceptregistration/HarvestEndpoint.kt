package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.api.CollectionsApi
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.difi.skos_ap_no.concept.builder.Conceptcollection.CollectionBuilder
import no.difi.skos_ap_no.concept.builder.ModelBuilder
import no.difi.skos_ap_no.concept.builder.generic.AudienceType
import org.apache.jena.rdf.model.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest


@RestController
class HarvestEndpoint(val sqlStore: SqlStore) : CollectionsApi {

    @Value("\${application.baseURL}")
    lateinit var baseURL: String

    private val logger = LoggerFactory.getLogger(HarvestEndpoint::class.java)

    override fun getCollections(httpServletRequest: HttpServletRequest, publisher: String): ResponseEntity<Any> {
        logger.info("Harvest - request")

        val allPublishedBegrepByCompany = sqlStore.getBegrepByCompany(publisher, Status.PUBLISERT)

        logger.info("Harvest found ${allPublishedBegrepByCompany.size} Begrep")

        var collectionBuilder = ModelBuilder.builder()
                                    .collectionBuilder("https://registrering-begrep.ut1.fellesdatakatalog.brreg.no/")
                                        .publisher(publisher)
                                        .name(publisher + " sin samling")

        for (begrep in allPublishedBegrepByCompany) {
            appendBegrepToCollection(begrep, collectionBuilder)
        }

        val writer = StringWriter()
        collectionBuilder.build().build().write(writer, "TURTLE")
        println(writer.buffer)

        return ResponseEntity.ok(writer.buffer.toString())
    }

    fun appendBegrepToCollection(begrep: Begrep, collectionBuilder: CollectionBuilder): Resource {
        var sourceItself = if (begrep.kildebeskrivelse != null && begrep.kildebeskrivelse.kilde != null && begrep.kildebeskrivelse.kilde.size > 0) begrep.kildebeskrivelse.kilde[0].uri + begrep.kildebeskrivelse.kilde[0].tekst else ""
        val urlForAccessingThisBegrepsRegistration = baseURL + begrep.ansvarligVirksomhet.id + "/" + begrep.id
        var builder = collectionBuilder.conceptBuilder(urlForAccessingThisBegrepsRegistration)
                .publisher(begrep.ansvarligVirksomhet.id)
                .definitionBuilder()
                    .text(begrep.definisjon, "nb")
                    .sourcedescriptionBuilder()
                        .sourceBuilder()
                            .label(sourceItself, "nb")
                            .seeAlso(begrep.kildebeskrivelse?.forholdTilKilde?.value ?: "")
                            .build()
                        .build()
                    .audience(AudienceType.Audience.Public)
                    .scopeNote(begrep.merknad ?: "", "nb")
                    .scopeBuilder()
                        .label(begrep.omfang?.tekst ?: "", "nb")
                        .seeAlso(begrep.omfang?.uri)
                        .build()
                    .modified(begrep.gyldigFom)
                    .build()
                .identifier(begrep.id)
                .prefLabelBuilder()
                    .label(begrep.anbefaltTerm ?: "", "no")
                    .build()
                .example(begrep.eksempel, "nb")
                .subject(begrep.fagområde, "nb")
                .contactPointBuilder()
                    .email(begrep.kontaktpunkt?.harEpost ?: "")
                    .telephone(begrep.kontaktpunkt?.harTelefon ?: "")
                    .build()

        begrep.bruksområde.forEach { bruksområde -> builder = builder.domainOfUse(bruksområde, "nb") }

        var altLabelBuilder = builder.altLabelBuilder()
        begrep.tillattTerm.forEach { tillattTerm -> altLabelBuilder = altLabelBuilder.label(tillattTerm, "nb") }
        builder = altLabelBuilder.build()

        var hiddenLabelBuilder = builder.hiddenLabelBuilder()
        begrep.frarådetTerm.forEach { frarådetTerm -> hiddenLabelBuilder = hiddenLabelBuilder.label(frarådetTerm, "nb") }
        builder = hiddenLabelBuilder.build()

        return builder.resource
    }
}