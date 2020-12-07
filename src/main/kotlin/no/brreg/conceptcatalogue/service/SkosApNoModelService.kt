package no.brreg.conceptcatalogue.service

import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Kildebeskrivelse
import no.difi.skos_ap_no.concept.builder.Conceptcollection.CollectionBuilder
import no.difi.skos_ap_no.concept.builder.Conceptcollection.Concept.ConceptBuilder
import no.difi.skos_ap_no.concept.builder.Conceptcollection.Concept.Sourcedescription.Definition.DefinitionBuilder
import no.difi.skos_ap_no.concept.builder.ModelBuilder
import no.difi.skos_ap_no.concept.builder.generic.SourceType
import org.apache.jena.rdf.model.Model
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.StringWriter

@Service
class SkosApNoModelService(
        private val conceptService: ConceptService
) {
    private val logger = LoggerFactory.getLogger(SkosApNoModelService::class.java)

    private val NB = "nb"

    @Value("\${application.collectionBaseUri}")
    private lateinit var collectionBaseUri: String

    fun buildModelForPublishersCollection(publisherId: String): Model {
        logger.info("Building concept collection model for publisher: {}", publisherId)

        val modelBuilder = instantiateModelBuilder()

        addConceptsToCollection(modelBuilder, publisherId)

        return modelBuilder.build()
    }

    fun buildModelForAllCollections(): Model {
        logger.info("Building concept collection models for all publishers")

        val modelBuilder = instantiateModelBuilder()

        conceptService
                .getAllPublisherIds()
                .forEach { addConceptsToCollection(modelBuilder, it) }

        return modelBuilder.build()
    }

    fun serializeAsTextTurtle(model: Model): String {
        val stringWriter = StringWriter()

        model.write(stringWriter, "TURTLE")

        return stringWriter.buffer.toString()
    }

    private fun instantiateModelBuilder(): ModelBuilder {
        return ModelBuilder.builder()
    }

    private fun instantiateCollectionBuilder(modelBuilder: ModelBuilder, publisherId: String): CollectionBuilder {
        return modelBuilder
                .collectionBuilder(getCollectionUri(publisherId))
                .publisher(publisherId)
                .name("Concept collection belonging to $publisherId")
    }

    private fun addConceptsToCollection(modelBuilder: ModelBuilder, publisherId: String) {
        val collectionBuilder = instantiateCollectionBuilder(modelBuilder, publisherId)

        conceptService
                .getPublishedConceptsForPublisherId(publisherId)
                .forEach { addConceptToCollection(collectionBuilder, it) }
    }

    private fun addConceptToCollection(collectionBuilder: CollectionBuilder, concept: Begrep) {
        val conceptBuilder = collectionBuilder
                .conceptBuilder(getConceptUri(collectionBuilder, concept.id))
                .identifier(concept.id)
                .publisher(concept.ansvarligVirksomhet.id)
                .modified(concept.endringslogelement.endringstidspunkt.toLocalDate())

        addPrefLabelToConcept(conceptBuilder, concept)
        addDefinitionToConcept(conceptBuilder, concept)
        addAltLabelToConcept(conceptBuilder, concept)
        addHiddenLabelToConcept(conceptBuilder, concept)
        addExampleToConcept(conceptBuilder, concept)
        addSubjectToConcept(conceptBuilder, concept)
        addDomainOfUseToConcept(conceptBuilder, concept)
        addContactPointToConcept(conceptBuilder, concept)
        addSeeAlsoReferencesToConcept(conceptBuilder, concept)
        addValidityPeriodToConcept(conceptBuilder, concept)

        conceptBuilder.build()
    }

    private fun getCollectionUri(publisherId: String): String {
        return "$collectionBaseUri/$publisherId"
    }

    private fun getConceptUri(collectionBuilder: CollectionBuilder, conceptId: String): String {
        return "${getCollectionUri(collectionBuilder.publisher)}/$conceptId"
    }

    private fun addPrefLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.anbefaltTerm?.navn
                ?.filterValues { !it?.toString().isNullOrBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    val prefLabelBuilder = conceptBuilder.prefLabelBuilder()

                    it.forEach { (key, value) -> prefLabelBuilder.label(value.toString(), key) }

                    prefLabelBuilder.build()
                }
    }


    private fun addDefinitionToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.definisjon?.tekst
                ?.filterValues { !it?.toString().isNullOrBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    val definitionBuilder = conceptBuilder.definitionBuilder()

                    it.forEach { (key, value) -> definitionBuilder.text(value.toString(), key) }

                    addScopeToDefinition(definitionBuilder, concept)
                    addScopeNoteToDefinition(definitionBuilder, concept)
                    addSourceDescriptionToDefinition(definitionBuilder, concept)

                    definitionBuilder.build()
                }
    }

    private fun addScopeToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.omfang
                ?.takeIf { it.tekst?.isNotBlank() == true || it.uri?.isNotBlank() == true }
                ?.let { definitionBuilder.scopeBuilder().label(it.tekst, NB).seeAlso(it.uri).build() }
    }

    private fun addScopeNoteToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.merknad
                ?.filterValues { !it?.toString().isNullOrBlank() }
                ?.forEach { (key, value) -> definitionBuilder.scopeNote(value.toString(), key) }
    }

    private fun addSourceDescriptionToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.kildebeskrivelse
                ?.takeIf { it.forholdTilKilde !== null || it.kilde.isNotEmpty() }
                ?.let {
                    val sourceDescriptionBuilder = definitionBuilder.sourcedescriptionBuilder()

                    it.forholdTilKilde?.let { type ->
                        if (type == Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT) {
                            sourceDescriptionBuilder.sourcetype(SourceType.Source.Userdefined)
                        }
                        if (type == Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE) {
                            sourceDescriptionBuilder.sourcetype(SourceType.Source.BasedOn)
                        }
                        if (type == Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE) {
                            sourceDescriptionBuilder.sourcetype(SourceType.Source.QuoteFrom)
                        }
                    }

                    val sourceBuilder = sourceDescriptionBuilder.sourceBuilder()

                    it.kilde
                            ?.filterNotNull()
                            ?.forEach { source -> sourceBuilder.label(source.tekst, NB).seeAlso(source.uri) }

                    sourceBuilder.build()

                    sourceDescriptionBuilder.build()
                }
    }

    private fun addAltLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.tillattTerm
                ?.filterValues { it is List<*> && it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    val altLabelBuilder = conceptBuilder.altLabelBuilder()

                    it.forEach { (key, entry) -> (entry as List<*>).forEach { value -> altLabelBuilder.label(value.toString(), key) } }

                    altLabelBuilder.build()
                }
    }

    private fun addHiddenLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.frarådetTerm
                ?.filterValues { it is List<*> && it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    val hiddenLabelBuilder = conceptBuilder.hiddenLabelBuilder()

                    it.forEach { (key, entry) -> (entry as List<*>).forEach { value -> hiddenLabelBuilder.label(value.toString(), key) } }

                    hiddenLabelBuilder.build()
                }
    }

    private fun addExampleToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.eksempel
                ?.filterValues { !it?.toString().isNullOrBlank() }
                ?.forEach { (key, value) -> conceptBuilder.example(value.toString(), key) }
    }

    private fun addSubjectToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.fagområde
                ?.filterValues { !it?.toString().isNullOrBlank() }
                ?.forEach { (key, value) -> conceptBuilder.subject(value.toString(), key) }
    }

    private fun addDomainOfUseToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.bruksområde
                ?.filterValues { it is List<*> && it.isNotEmpty() }
                ?.forEach { (key, entry) -> (entry as List<*>).forEach { value -> conceptBuilder.domainOfUse(value.toString(), key) } }
    }

    private fun addContactPointToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.kontaktpunkt
                ?.let {
                    val contactPointBuilder = conceptBuilder.contactPointBuilder()

                    val email = it.harEpost
                    val phone = it.harTelefon

                    if (email?.isNotBlank() == true) {
                        contactPointBuilder.email(email)
                    }

                    if (phone?.isNotBlank() == true) {
                        contactPointBuilder.telephone(phone)
                    }

                    contactPointBuilder.build()
                }
    }

    private fun addSeeAlsoReferencesToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.seOgså
                ?.filter { !it.isNullOrBlank() }
                ?.forEach { conceptBuilder.seeAlso(it).build() }
    }

    private fun addValidityPeriodToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val validFromIncluding = concept.gyldigFom;
        val validToIncluding = concept.gyldigTom;

        if (validFromIncluding != null && validToIncluding != null) {
            if (validFromIncluding.isBefore(validToIncluding)) {
                conceptBuilder
                        .periodOfTimeBuilder()
                        .validFromIncluding(validFromIncluding)
                        .validToIncluding(validToIncluding)
                        .build()
            }
        } else {
            if (validFromIncluding != null) {
                conceptBuilder
                        .periodOfTimeBuilder()
                        .validFromIncluding(validFromIncluding)
                        .build()
            }
            if (validToIncluding != null) {
                conceptBuilder
                        .periodOfTimeBuilder()
                        .validToIncluding(validToIncluding)
                        .build()
            }
        }
    }
}
