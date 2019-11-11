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
        conceptService.getAllPublisherIds().forEach { addConceptsToCollection(modelBuilder, it) }
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
        val concepts = conceptService.getPublishedConceptsForPublisherId(publisherId)
        concepts.forEach { addConceptToCollection(collectionBuilder, it) }
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

        conceptBuilder.build()
    }

    private fun getCollectionUri(publisherId: String): String {
        return "$collectionBaseUri/$publisherId"
    }

    private fun getConceptUri(collectionBuilder: CollectionBuilder, conceptId: String): String {
        return "${getCollectionUri(collectionBuilder.publisher)}/$conceptId"
    }

    private fun addPrefLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val term = concept.anbefaltTerm
        if (term != null && term.navn.isNotEmpty()) {
            val prefLabelBuilder = conceptBuilder.prefLabelBuilder()
            term.navn.forEach { (key, value) ->
                if (value != null && value.toString().trim().isNotEmpty()) {
                    prefLabelBuilder.label(value.toString(), key)
                }
            }
            prefLabelBuilder.build()
        }
    }

    private fun addDefinitionToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val definition = concept.definisjon
        if (definition != null && definition.tekst.isNotEmpty()) {
            val definitionBuilder = conceptBuilder.definitionBuilder()
            definition.tekst.forEach { (key, value) ->
                if (value != null && value.toString().trim().isNotEmpty()) {
                    definitionBuilder.text(value.toString(), key)
                }
            }
            addScopeToDefinition(definitionBuilder, concept)
            addScopeNoteToDefinition(definitionBuilder, concept)
            addSourceDescriptionToDefinition(definitionBuilder, concept)
            definitionBuilder.build()
        }
    }

    private fun addScopeToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        val scope = concept.omfang
        if (scope != null && (scope.tekst.trim().isNotEmpty() || scope.uri.trim().isNotEmpty())) {
            definitionBuilder.scopeBuilder().label(scope.tekst, NB).seeAlso(scope.uri).build()
        }
    }

    private fun addScopeNoteToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        val scopeNote = concept.merknad
        if (scopeNote != null && scopeNote.isNotEmpty()) {
            scopeNote.forEach { (key, value) ->
                if (value != null && value.toString().trim().isNotEmpty()) {
                    definitionBuilder.scopeNote(value.toString(), key)
                }
            }
        }
    }

    private fun addSourceDescriptionToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        val sourceDescription = concept.kildebeskrivelse
        if (sourceDescription != null) {
            val sourcedescriptionBuilder = definitionBuilder.sourcedescriptionBuilder()
            sourceDescription.forholdTilKilde?.let {
                if (it == Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT) {
                    sourcedescriptionBuilder.sourcetype(SourceType.Source.Userdefined)
                }
                if (it == Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE) {
                    sourcedescriptionBuilder.sourcetype(SourceType.Source.BasedOn)
                }
                if (it == Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE) {
                    sourcedescriptionBuilder.sourcetype(SourceType.Source.QuoteFrom)
                }
            }
            val sources = sourceDescription.kilde
            if (sources.isNotEmpty()) {
                val sourceBuilder = sourcedescriptionBuilder.sourceBuilder()
                sources.forEach { source -> sourceBuilder.label(source.tekst, NB).seeAlso(source.uri) }
                sourceBuilder.build()
            }
            sourcedescriptionBuilder.build()
        }
    }

    private fun addAltLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val altLabel = concept.tillattTerm
        if (altLabel != null && altLabel.isNotEmpty()) {
            val altLabelBuilder = conceptBuilder.altLabelBuilder()
            altLabel.forEach { (key, entry) ->
                if (entry is List<*> && entry.isNotEmpty()) {
                    entry.forEach { value -> altLabelBuilder.label(value.toString(), key) }
                }
            }
            altLabelBuilder.build()
        }
    }

    private fun addHiddenLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val hiddenLabel = concept.frarådetTerm
        if (hiddenLabel != null && hiddenLabel.isNotEmpty()) {
            val hiddenLabelBuilder = conceptBuilder.hiddenLabelBuilder()
            hiddenLabel.forEach { (key, entry) ->
                if (entry is List<*> && entry.isNotEmpty()) {
                    entry.forEach { value -> hiddenLabelBuilder.label(value.toString(), key) }
                }
            }
            hiddenLabelBuilder.build()
        }
    }

    private fun addExampleToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val example = concept.eksempel
        if (example != null && example.isNotEmpty()) {
            example.forEach { (key, value) ->
                if (value != null && value.toString().trim().isNotEmpty()) {
                    conceptBuilder.example(value.toString(), key)
                }
            }
        }
    }

    private fun addSubjectToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val subject = concept.fagområde
        if (subject != null && subject.isNotEmpty()) {
            subject.forEach { (key, value) ->
                if (value != null && value.toString().trim().isNotEmpty()) {
                    conceptBuilder.subject(value.toString(), key)
                }
            }
        }
    }

    private fun addDomainOfUseToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val domainOfUse = concept.bruksområde
        if (domainOfUse != null && domainOfUse.isNotEmpty()) {
            domainOfUse.forEach { (key, entry) ->
                if (entry is List<*> && entry.isNotEmpty()) {
                    entry.forEach { value -> conceptBuilder.domainOfUse(value.toString(), key) }
                }
            }
        }
    }

    private fun addContactPointToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val contactPoint = concept.kontaktpunkt
        if (contactPoint != null) {
            val contactPointBuilder = conceptBuilder.contactPointBuilder()
            val email = contactPoint.harEpost
            val phone = contactPoint.harTelefon
            if (email != null && email.trim().isNotEmpty()) {
                contactPointBuilder.email(email)
            }
            if (phone != null && phone.trim().isNotEmpty()) {
                contactPointBuilder.telephone(phone)
            }
            contactPointBuilder.build()
        }
    }
}
