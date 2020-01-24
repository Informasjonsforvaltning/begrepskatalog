package no.brreg.conceptcatalogue.service

import no.begrepskatalog.generated.model.*
import no.difi.skos_ap_no.concept.builder.Conceptcollection.CollectionBuilder
import no.difi.skos_ap_no.concept.builder.ModelBuilder
import no.difi.skos_ap_no.concept.builder.SKOSNO
import no.difi.skos_ap_no.concept.builder.generic.SourceType
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.internal.util.reflection.FieldSetter.setField
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.StringReader
import java.lang.reflect.InvocationTargetException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner::class)
class SkosApNoModelServiceTest {
    @Autowired
    private lateinit var skosApNoModelService: SkosApNoModelService

    @Value("\${application.collectionBaseUri}")
    private lateinit var collectionBaseUri: String

    private val publisherUriPrefix = "https://data.brreg.no/enhetsregisteret/api/enheter/"
    private val validLanguageCodes = listOf("nb", "nn", "en")

    @Test
    fun expectEmptyModelToBeSerialisedAsTextTurtleCorrectly() {
        val model = createEmptyModelBuilder().build()
        val serialisedModel = skosApNoModelService.serializeAsTextTurtle(model)
        val deserialisedModel = ModelBuilder.builder().build().read(StringReader(serialisedModel), null, "TURTLE")

        assertTrue("Expect empty model to be serialised correctly in TURTLE format", deserialisedModel.isIsomorphicWith(model))
        assertTrue("Expect deserialised model to be empty", deserialisedModel.isEmpty)
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectCollectionUriToBeCorrect() {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("getCollectionUri", String::class.java)
        method.isAccessible = true

        val publisherId = "123456789"

        val uri = method.invoke(skosApNoModelService, publisherId) as String

        assertNotNull("Expect collection URI to be created", uri)
        assertEquals("Expect collection URI to be correct", createExpectedCollectionUri(publisherId), uri)
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectConceptUriToBeCorrect() {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("getConceptUri", CollectionBuilder::class.java, String::class.java)
        method.isAccessible = true

        val publisherId = "123456789"
        val conceptId = "concept-id"

        val collectionBuilder = mock(CollectionBuilder::class.java)
        `when`(collectionBuilder.publisher).thenReturn(publisherId)

        val uri = method.invoke(skosApNoModelService, collectionBuilder, conceptId) as String

        assertNotNull("Expect concept URI to be created", uri)
        assertEquals("Expect concept URI to be correct", "${createExpectedCollectionUri(publisherId)}/$conceptId", uri)
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectModelBuilderToInstantiateEmptyModel() {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("instantiateModelBuilder")
        method.isAccessible = true

        val modelBuilder = method.invoke(skosApNoModelService) as ModelBuilder

        assertTrue("Expect model builder to instantiate an empty model", modelBuilder.model.isEmpty)
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectCollectionBuilderToInstantiateModelWithSingleCollectionCorrectly() {
        val publisherId = "123456789"

        val modelBuilder = createEmptyModelBuilder()

        assertTrue("Expect model to be empty", modelBuilder.model.isEmpty)

        val collectionBuilder = createEmptyCollectionBuilder(modelBuilder, publisherId)

        assertFalse("Expect model not to be empty", collectionBuilder.model.isEmpty)
        assertNotNull("Expect collection builder to be defined", collectionBuilder)
        assertEquals("Expect model to contain only one collection", 1, collectionBuilder.model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect collection publisher ID to be set correctly", publisherId, collectionBuilder.publisher)
        assertEquals("Expect collection namespace to be set correctly", createExpectedCollectionUri(publisherId), collectionBuilder.resource.nameSpace)
        assertEquals("Expect collection to not contain any concepts", 0, collectionBuilder.model.listStatements(null, RDF.type, SKOS.Concept).toList().size)
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectCollectionBuilderToInstantiateModelWithMultipleCollectionsCorrectly() {
        val publisherId1 = "123456789"
        val publisherId2 = "987654321"

        val modelBuilder = createEmptyModelBuilder()

        assertTrue("Expect model to be empty", modelBuilder.model.isEmpty)

        val collectionBuilder1 = createEmptyCollectionBuilder(modelBuilder, publisherId1)

        assertFalse("Expect model not to be empty", modelBuilder.model.isEmpty)
        assertNotNull("Expect collection builder to be defined", collectionBuilder1)
        assertEquals("Expect collection publisher ID to be set correctly", publisherId1, collectionBuilder1.publisher)
        assertEquals("Expect collection namespace to be set correctly", createExpectedCollectionUri(publisherId1), collectionBuilder1.resource.nameSpace)
        assertEquals("Expect collection not to contain any concepts", 0, collectionBuilder1.model.listStatements(null, RDF.type, SKOS.Concept).toList().size)

        val collectionBuilder2 = createEmptyCollectionBuilder(modelBuilder, publisherId2)

        assertFalse("Expect model not to be empty", modelBuilder.model.isEmpty)
        assertNotNull("Expect collection builder to be defined", collectionBuilder2)
        assertEquals("Expect collection publisher ID to be set correctly", publisherId2, collectionBuilder2.publisher)
        assertEquals("Expect collection namespace to be set correctly", createExpectedCollectionUri(publisherId2), collectionBuilder2.resource.nameSpace)
        assertEquals("Expect collection not to contain any concepts", 0, collectionBuilder2.model.listStatements(null, RDF.type, SKOS.Concept).toList().size)

        val collectionStatements = modelBuilder.model.listStatements(null, RDF.type, SKOS.Collection).toList()

        assertEquals("Expect model to contain exactly two collections", 2, collectionStatements.size)

        val expectedCollectionNamespaces = Stream.of(publisherId1, publisherId2).map { createExpectedCollectionUri(it) }.collect(Collectors.toList())
        val actualCollectionNamespaces = collectionStatements.stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList())

        assertTrue("Expect model to contain collections with correct namespaces", actualCollectionNamespaces.containsAll(expectedCollectionNamespaces))
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun expectMultipleConceptsToBeCorrectlyAddedToSameCollection() {
        val publisherId = "123456789"
        val conceptIds = listOf("concept-id-1", "concept-id-2")

        val concepts = conceptIds
                .stream()
                .map { conceptId -> createConceptForPublisher(publisherId, conceptId) }
                .collect(Collectors.toList())

        val collectionBuilder = createEmptyCollectionBuilder(createEmptyModelBuilder(), publisherId)
        concepts.forEach { concept ->
            try {
                addConceptToCollection(collectionBuilder, concept)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        assertNotNull("Expect collection builder to be defined", collectionBuilder)

        val model = collectionBuilder.model

        assertNotNull("Expect model to be defined", model)
        assertFalse("Expect model not to be empty", model.isEmpty)
        assertEquals("Expect model to have only one collection", 1, model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect model to have the correct number of concepts", conceptIds.size, model.listStatements(null, RDF.type, SKOS.Concept).toList().size)

        val expectedConceptNamespaces = conceptIds.stream().map { conceptId -> createExpectedConceptUri(collectionBuilder, conceptId) }.collect(Collectors.toList())

        collectionBuilder.resource.listProperties(SKOS.member).toList().forEach { statement ->
            assertEquals("Expect concept to belong to the correct collection", createExpectedCollectionUri(publisherId), statement.subject.toString())
            assertTrue("Expect concept to have correct namespace", expectedConceptNamespaces.contains(statement.getObject().toString()))
        }

        model.listStatements(null, RDF.type, SKOS.Concept).toList().forEach { conceptStatement ->
            val conceptResource = conceptStatement.subject
            val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()
            concepts.stream().filter { concept -> concept.id == expectedIdentifier }.findAny().ifPresent { concept -> validateConceptSerialisation(conceptResource, concept) }
        }
    }

    @Test
    fun expectMultipleConceptsToBeCorrectlyAddedToDifferentCollections() {
        val mapOfPublishersAndConcepts = mapOf(
                "123456789" to listOf("publisher-a-concept-1"),
                "987654321" to listOf("publisher-b-concept-1", "publisher-b-concept-2")
        )

        val concepts = mutableListOf<Begrep>()

        val expectedCollectionNamespaces = mapOfPublishersAndConcepts.keys.stream().map { createExpectedCollectionUri(it) }.collect(Collectors.toList())
        val expectedConceptNamespaces = mutableListOf<String>()

        val mapOfExpectedCollectionAndConceptNamespaces: Map<String, MutableList<String>> = mapOfPublishersAndConcepts.entries.stream().collect(Collectors.toMap({ entry -> createExpectedCollectionUri(entry.key) }, { mutableListOf() }))

        val modelBuilder = createEmptyModelBuilder()

        mapOfPublishersAndConcepts.forEach { (key, value) ->
            val publishersConcepts = value.stream().map { conceptId -> createConceptForPublisher(key, conceptId) }.collect(Collectors.toList())
            concepts.addAll(publishersConcepts)

            try {
                val collectionBuilder = createEmptyCollectionBuilder(modelBuilder, key)
                publishersConcepts.forEach { concept ->
                    try {
                        addConceptToCollection(collectionBuilder, concept)
                        val expectedConceptNamespace = createExpectedConceptUri(collectionBuilder, concept.id)
                        expectedConceptNamespaces.add(expectedConceptNamespace)
                        mapOfExpectedCollectionAndConceptNamespaces[createExpectedCollectionUri(key)]?.add(expectedConceptNamespace)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                assertNotNull("Expect collection builder to be defined", collectionBuilder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val model = modelBuilder.model

        assertNotNull("Expect model to be defined", model)
        assertFalse("Expect model not to be empty", model.isEmpty)
        assertEquals("Expect model to have the correct number of collections", mapOfPublishersAndConcepts.size, model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect model to have the correct total number of concepts", concepts.size, model.listStatements(null, RDF.type, SKOS.Concept).toList().size)
        assertTrue("Expect all collections to be in the list of expected collections", model.listStatements(null, RDF.type, SKOS.Collection).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedCollectionNamespaces))
        assertTrue("Expect all concepts to be in the list of expected concepts", model.listStatements(null, RDF.type, SKOS.Concept).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedConceptNamespaces))

        model.listStatements(null, RDF.type, SKOS.Collection).toList().forEach { collection ->
            assertEquals("Expect collection to contain the correct number of concepts", mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]?.size, collection.subject.listProperties(SKOS.member).toList().size)
            assertTrue("Expect collection to contain the correct concepts", collection.subject.listProperties(SKOS.member).toList().stream().map { it.getObject() }.map { Objects.toString(it) }.collect(Collectors.toList()).containsAll(mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]!!))
        }

        model.listStatements(null, RDF.type, SKOS.Concept).toList().forEach { conceptStatement ->
            val conceptResource = conceptStatement.subject
            val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()
            concepts.stream().filter { concept -> concept.id == expectedIdentifier }.findAny().ifPresent { concept -> validateConceptSerialisation(conceptResource, concept) }
        }
    }

    @Test
    fun expectMultipleConceptsToBeCorrectlyAddedToDifferentCollectionsAfterGettingConceptsForPublisher() {
        val mapOfPublishersAndConcepts = mapOf(
                "123456789" to listOf("publisher-a-concept-1"),
                "987654321" to listOf("publisher-b-concept-1", "publisher-b-concept-2")
        )

        val concepts = mutableListOf<Begrep>()

        val expectedCollectionNamespaces = mapOfPublishersAndConcepts.keys.stream().map { createExpectedCollectionUri(it) }.collect(Collectors.toList())
        val expectedConceptNamespaces = mutableListOf<String>()

        val mapOfExpectedCollectionAndConceptNamespaces: Map<String, MutableList<String>> = mapOfPublishersAndConcepts.entries.stream().collect(Collectors.toMap({ entry -> createExpectedCollectionUri(entry.key) }, { mutableListOf() }))

        val modelBuilder = createEmptyModelBuilder()

        val conceptService = mock(ConceptService::class.java)
        val collectionBuilder = mock(CollectionBuilder::class.java)

        mapOfPublishersAndConcepts.forEach { (key, value) ->
            val publishersConcepts = value.stream().map { conceptId -> createConceptForPublisher(key, conceptId) }.collect(Collectors.toList())
            concepts.addAll(publishersConcepts)

            `when`(conceptService.getPublishedConceptsForPublisherId(key)).thenReturn(publishersConcepts)
            `when`(collectionBuilder.publisher).thenReturn(key)

            try {
                setField(skosApNoModelService, skosApNoModelService.javaClass.getDeclaredField("conceptService"), conceptService)

                addConceptsToCollection(modelBuilder, key)
                value.forEach { concept ->
                    val expectedConceptNamespace = createExpectedConceptUri(collectionBuilder, concept)
                    expectedConceptNamespaces.add(expectedConceptNamespace)
                    mapOfExpectedCollectionAndConceptNamespaces[createExpectedCollectionUri(key)]?.add(expectedConceptNamespace)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val model = modelBuilder.model

        assertNotNull("Expect model to be defined", model)
        assertFalse("Expect model not to be empty", model.isEmpty)
        assertEquals("Expect model to have the correct number of collections", mapOfPublishersAndConcepts.size, model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect model to have the correct total number of concepts", concepts.size, model.listStatements(null, RDF.type, SKOS.Concept).toList().size)
        assertTrue("Expect all collections to be in the list of expected collections", model.listStatements(null, RDF.type, SKOS.Collection).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedCollectionNamespaces))
        assertTrue("Expect all concepts to be in the list of expected concepts", model.listStatements(null, RDF.type, SKOS.Concept).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedConceptNamespaces))

        model.listStatements(null, RDF.type, SKOS.Collection).toList().forEach { collection ->
            assertEquals("Expect collection to contain the correct number of concepts", mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]?.size, collection.subject.listProperties(SKOS.member).toList().size)
            assertTrue("Expect collection to contain the correct concepts", collection.subject.listProperties(SKOS.member).toList().stream().map { it.getObject() }.map { Objects.toString(it) }.collect(Collectors.toList()).containsAll(mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]!!))
        }

        model.listStatements(null, RDF.type, SKOS.Concept).toList().forEach { conceptStatement ->
            val conceptResource = conceptStatement.subject
            val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()
            concepts.stream().filter { concept -> concept.id == expectedIdentifier }.findAny().ifPresent { concept -> validateConceptSerialisation(conceptResource, concept) }
        }
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectCorrectModelToBeBuiltForGivenPublishersCollection() {
        val publisherId = "123456789"
        val conceptIds = listOf("publisher-a-concept-1", "publisher-a-concept-2")

        val concepts = conceptIds.stream().map { conceptId -> createConceptForPublisher(publisherId, conceptId) }.collect(Collectors.toList())

        val collectionBuilder = mock(CollectionBuilder::class.java)
        `when`(collectionBuilder.publisher).thenReturn(publisherId)

        val expectedCollectionNamespaces = Stream.of(publisherId).map { createExpectedCollectionUri(it) }.collect(Collectors.toList())
        val expectedConceptNamespaces = conceptIds.stream().map { conceptId -> createExpectedConceptUri(collectionBuilder, conceptId) }.collect(Collectors.toList())

        val conceptService = mock(ConceptService::class.java)
        `when`(conceptService.getPublishedConceptsForPublisherId(publisherId)).thenReturn(concepts)
        setField(skosApNoModelService, skosApNoModelService.javaClass.getDeclaredField("conceptService"), conceptService)

        val model = skosApNoModelService.buildModelForPublishersCollection(publisherId)

        assertNotNull("Expect model to be defined", model)
        assertFalse("Expect model not to be empty", model.isEmpty)
        assertEquals("Expect model to have only one collections", 1, model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect model to have the correct total number of concepts", conceptIds.size, model.listStatements(null, RDF.type, SKOS.Concept).toList().size)
        assertTrue("Expect all collections to be in the list of expected collections", model.listStatements(null, RDF.type, SKOS.Collection).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedCollectionNamespaces))
        assertTrue("Expect all concepts to be in the list of expected concepts", model.listStatements(null, RDF.type, SKOS.Concept).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedConceptNamespaces))

        model.listStatements(null, RDF.type, SKOS.Collection).toList().forEach { collection ->
            assertEquals("Expect collection to contain the correct number of concepts", conceptIds.size, collection.subject.listProperties(SKOS.member).toList().size)
            assertTrue("Expect collection to contain the correct concepts", collection.subject.listProperties(SKOS.member).toList().stream().map { it.getObject() }.map { Objects.toString(it) }.collect(Collectors.toList()).containsAll(expectedConceptNamespaces))
        }

        model.listStatements(null, RDF.type, SKOS.Concept).toList().forEach { conceptStatement ->
            val conceptResource = conceptStatement.subject
            val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()
            concepts.stream().filter { concept -> concept.id == expectedIdentifier }.findAny().ifPresent { concept -> validateConceptSerialisation(conceptResource, concept) }
        }
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectCorrectModelToBeBuiltForAllCollections() {
        val mapOfPublishersAndConcepts = mapOf(
                "123456789" to listOf("publisher-a-concept-1"),
                "987654321" to listOf("publisher-b-concept-1", "publisher-b-concept-2")
        )

        val concepts = mutableListOf<Begrep>()

        val expectedCollectionNamespaces = mapOfPublishersAndConcepts.keys.stream().map { createExpectedCollectionUri(it) }.collect(Collectors.toList())
        val expectedConceptNamespaces = mutableListOf<String>()

        val mapOfExpectedCollectionAndConceptNamespaces: Map<String, MutableList<String>> = mapOfPublishersAndConcepts.entries.stream().collect(Collectors.toMap({ entry -> createExpectedCollectionUri(entry.key) }, { mutableListOf() }))

        val conceptService = mock(ConceptService::class.java)
        val collectionBuilder = mock(CollectionBuilder::class.java)
        `when`(conceptService.getAllPublisherIds()).thenReturn(ArrayList(mapOfPublishersAndConcepts.keys))

        mapOfPublishersAndConcepts.forEach { (key, value) ->
            val publishersConcepts = value.stream().map { conceptId -> createConceptForPublisher(key, conceptId) }.collect(Collectors.toList())
            concepts.addAll(publishersConcepts)

            `when`(conceptService.getPublishedConceptsForPublisherId(key)).thenReturn(publishersConcepts)
            `when`(collectionBuilder.publisher).thenReturn(key)

            value.forEach { concept ->
                val expectedConceptNamespace = createExpectedConceptUri(collectionBuilder, concept)
                expectedConceptNamespaces.add(expectedConceptNamespace)
                mapOfExpectedCollectionAndConceptNamespaces[createExpectedCollectionUri(key)]?.add(expectedConceptNamespace)
            }
        }

        setField(skosApNoModelService, skosApNoModelService.javaClass.getDeclaredField("conceptService"), conceptService)

        val model = skosApNoModelService.buildModelForAllCollections()

        assertNotNull("Expect model to be defined", model)
        assertFalse("Expect model not to be empty", model.isEmpty)
        assertEquals("Expect model to have the correct number of collections", mapOfPublishersAndConcepts.size, model.listStatements(null, RDF.type, SKOS.Collection).toList().size)
        assertEquals("Expect model to have the correct total number of concepts", concepts.size, model.listStatements(null, RDF.type, SKOS.Concept).toList().size)
        assertTrue("Expect all collections to be in the list of expected collections", model.listStatements(null, RDF.type, SKOS.Collection).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedCollectionNamespaces))
        assertTrue("Expect all concepts to be in the list of expected concepts", model.listStatements(null, RDF.type, SKOS.Concept).toList().stream().map { it.subject }.map { it.toString() }.collect(Collectors.toList()).containsAll(expectedConceptNamespaces))

        model.listStatements(null, RDF.type, SKOS.Collection).toList().forEach { collection ->
            assertEquals("Expect collection to contain the correct number of concepts", mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]?.size, collection.subject.listProperties(SKOS.member).toList().size)
            assertTrue("Expect collection to contain the correct concepts", collection.subject.listProperties(SKOS.member).toList().stream().map { it.getObject() }.map { Objects.toString(it) }.collect(Collectors.toList()).containsAll(mapOfExpectedCollectionAndConceptNamespaces[collection.subject.toString()]!!))
        }

        model.listStatements(null, RDF.type, SKOS.Concept).toList().forEach { conceptStatement ->
            val conceptResource = conceptStatement.subject
            val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()
            concepts.stream().filter { concept -> concept.id == expectedIdentifier }.findAny().ifPresent { concept -> validateConceptSerialisation(conceptResource, concept) }
        }
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithoutAnyPropertiesToThrowNullPointerException() {
        val publisherId = "123456789"

        val concept = Begrep()

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithOnlyIdToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithEmptyPublisherToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            ansvarligVirksomhet = Virksomhet()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = NullPointerException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithEmptyLastModifiedDateToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = Endringslogelement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithoutPrefLabelToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithEmptyPrefLabelToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = Term()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithoutDefinitionToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithEmptyDefinitionToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = Definisjon()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithOnlyMandatoryFieldsToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyScopeToThrowNullPointerException() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = URITekst()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyScopeToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyScopeNoteToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyScopeNoteToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptySourceDescriptionToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = Kildebeskrivelse()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptySourceDescriptionToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyAltLabelToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyAltLabelToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyHiddenLabelToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyHiddenLabelToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyExampleToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyExampleToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptySubjectToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptySubjectToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyDomainOfUseToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = HashMap()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyDomainOfUseToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptyContactPointToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
            kontaktpunkt = Kontaktpunkt()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptyContactPointToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
            kontaktpunkt = createContactPoint()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndEmptySeeAlsoReferencesToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
            kontaktpunkt = createContactPoint()
            seOgså = emptyList()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    @Test
    @Throws(NoSuchFieldException::class)
    fun expectConceptWithMandatoryFieldsAndNonEmptySeeAlsoReferencesToBeSerialisedCorrectly() {
        val publisherId = "123456789"
        val conceptId = "concept-id"

        val concept = Begrep().apply {
            id = conceptId
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
            kontaktpunkt = createContactPoint()
            seOgså = createSeeAlsoReferences()
        }

        validateModelWithSingleConcept(publisherId, concept)
    }

    private fun createExpectedCollectionUri(publisherId: String): String {
        return "$collectionBaseUri/$publisherId"
    }

    private fun createExpectedConceptUri(collectionBuilder: CollectionBuilder, conceptId: String): String {
        return createExpectedCollectionUri(collectionBuilder.publisher) + "/" + conceptId
    }

    private fun createPublisher(publisherId: String): Virksomhet {
        return Virksomhet().apply {
            id = publisherId
        }
    }

    private fun createChangeLogElement(): Endringslogelement {
        return Endringslogelement().apply {
            endringstidspunkt = OffsetDateTime.now()
        }
    }

    private fun createPrefLabel(): Term {
        return Term().apply {
            navn = mapOf(
                    "nb" to "pref-label-nb",
                    "nn" to "pref-label-nn",
                    "en" to "pref-label-en"
            )
        }
    }

    private fun createDefinition(): Definisjon {
        return Definisjon().apply {
            tekst = mapOf(
                    "nb" to "definition-nb",
                    "nn" to "definition-nn",
                    "en" to "definition-en"
            )
        }
    }

    private fun createScope(): URITekst {
        return URITekst().apply {
            tekst = "scope-text"
            uri = "http://scope-uri.com"
        }
    }

    private fun createScopeNote(): Map<String, Any> {
        return mapOf(
                "nb" to "scope-note-nb",
                "nn" to "scope-note-nn",
                "en" to "scope-note-en"
        )
    }

    private fun createSourceDescription(): Kildebeskrivelse {
        return Kildebeskrivelse().apply {
            forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE
            kilde = listOf(
                    URITekst().apply {
                        tekst = "source-text-1"
                        uri = "http://localhost/source-uri-1"
                    },
                    URITekst().apply {
                        tekst = "source-text-2"
                        uri = "http://localhost/source-uri-2"
                    }
            )
        }
    }

    private fun createAltLabel(): Map<String, Any> {
        return mapOf(
                "nb" to listOf("alt-label-nb-1", "alt-label-nb-2"),
                "nn" to listOf("alt-label-nn-1", "alt-label-nn-2"),
                "en" to listOf("alt-label-en-1", "alt-label-en-2")
        )
    }

    private fun createHiddenLabel(): Map<String, Any> {
        return mapOf(
                "nb" to listOf("hidden-label-nb-1", "hidden-label-nb-2"),
                "nn" to listOf("hidden-label-nn-1", "hidden-label-nn-2"),
                "en" to listOf("hidden-label-en-1", "hidden-label-en-2")
        )
    }

    private fun createExample(): Map<String, Any> {
        return mapOf(
                "nb" to "example-nb",
                "nn" to "example-nn",
                "en" to "example-en"
        )
    }

    private fun createSubject(): Map<String, Any> {
        return mapOf(
                "nb" to "subject-nb",
                "nn" to "subject-nn",
                "en" to "subject-en"
        )
    }

    private fun createDomainOfUse(): Map<String, Any> {
        return mapOf(
                "nb" to listOf("domain-of-use-nb-1", "domain-of-use-nb-2"),
                "nn" to listOf("domain-of-use-nn-1", "domain-of-use-nn-2"),
                "en" to listOf("domain-of-use-en-1", "domain-of-use-en-2")
        )
    }

    private fun createContactPoint(): Kontaktpunkt {
        return Kontaktpunkt().apply {
            harEpost = "email@test.com"
            harTelefon = "123456789"
        }
    }

    private fun createSeeAlsoReferences(): List<String> = listOf(
            "http://localhost/see-also-reference-1",
            "http://localhost/see-also-reference-2"
    )

    private fun createConceptForPublisher(publisherId: String, conceptId: String): Begrep {
        return Begrep().apply {
            id = conceptId
            ansvarligVirksomhet = createPublisher(publisherId)
            endringslogelement = createChangeLogElement()
            anbefaltTerm = createPrefLabel()
            definisjon = createDefinition()
            omfang = createScope()
            merknad = createScopeNote()
            kildebeskrivelse = createSourceDescription()
            tillattTerm = createAltLabel()
            frarådetTerm = createHiddenLabel()
            eksempel = createExample()
            fagområde = createSubject()
            bruksområde = createDomainOfUse()
            kontaktpunkt = createContactPoint()
        }
    }

    private fun createEmptyModelBuilder(): ModelBuilder {
        return ModelBuilder.builder()
    }

    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    private fun createEmptyCollectionBuilder(modelBuilder: ModelBuilder, publisherId: String): CollectionBuilder {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("instantiateCollectionBuilder", ModelBuilder::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(skosApNoModelService, modelBuilder, publisherId) as CollectionBuilder
    }

    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    private fun addConceptToCollection(collectionBuilder: CollectionBuilder, concept: Begrep) {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("addConceptToCollection", CollectionBuilder::class.java, Begrep::class.java)
        method.isAccessible = true
        method.invoke(skosApNoModelService, collectionBuilder, concept)
    }

    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    private fun addConceptsToCollection(modelBuilder: ModelBuilder, publisherId: String) {
        val method = SkosApNoModelService::class.java.getDeclaredMethod("addConceptsToCollection", ModelBuilder::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(skosApNoModelService, modelBuilder, publisherId)
    }

    @Throws(NoSuchFieldException::class)
    private fun validateModelWithSingleConcept(publisherId: String, concept: Begrep) {
        val concepts = Stream.of(concept).collect(Collectors.toList())
        val conceptService = mock(ConceptService::class.java)
        `when`(conceptService.getPublishedConceptsForPublisherId(publisherId)).thenReturn(concepts)
        setField(skosApNoModelService, skosApNoModelService.javaClass.getDeclaredField("conceptService"), conceptService)
        skosApNoModelService.buildModelForPublishersCollection(publisherId).listStatements(null, RDF.type, SKOS.Concept).toList().stream().map { it.subject }.forEach { conceptResource -> validateConceptSerialisation(conceptResource, concept) }
    }

    private fun validateConceptSerialisation(conceptResource: Resource, concept: Begrep) {
        val expectedIdentifier = conceptResource.getProperty(DCTerms.identifier).literal.toString()

        assertNotNull("Expect concept to have an identifier", expectedIdentifier)
        assertEquals("Expect concept to have correct identifier", concept.id, expectedIdentifier)

        val expectedPublisherId = conceptResource.getProperty(DCTerms.publisher).resource.uri

        assertNotNull("Expect concept to have a publisher", expectedPublisherId)
        assertEquals("Expect concept to have correct publisher", publisherUriPrefix + concept.ansvarligVirksomhet.id, expectedPublisherId)

        val expectedLastModifiedDate = conceptResource.getProperty(DCTerms.modified).literal.string

        assertNotNull("Expect concept to have a last modified date", expectedLastModifiedDate)
        assertEquals("Expect concept to have correct last modified date", 0, LocalDate.parse(expectedLastModifiedDate, DateTimeFormatter.ISO_LOCAL_DATE).compareTo(LocalDate.now()))

        val prefLabelResource = conceptResource.getProperty(SKOSXL.prefLabel).resource
        val prefLabelLanguages = prefLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

        assertNotNull("Expect concept to have a recommended term", prefLabelResource)
        assertTrue("Expect concept recommended term to have a literal form", prefLabelResource.hasProperty(SKOSXL.literalForm))
        assertFalse("Expect concept to have recommended term in at least one language", prefLabelResource.listProperties(SKOSXL.literalForm).toList().isEmpty())
        assertTrue("Expect concept to have non-empty translations of recommended term", prefLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.literal }.map { it.string }.filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
        assertTrue("Expect concept to be translated to valid languages", validLanguageCodes.containsAll(prefLabelLanguages))

        val definitionResource = conceptResource.getProperty(SKOSNO.betydningsbeskrivelse).resource
        val definitionLanguages = definitionResource.listProperties(RDFS.label).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

        assertNotNull("Expect concept to have a definition", definitionResource)
        assertTrue("Expect concept definition to have a label", definitionResource.hasProperty(RDFS.label))
        assertFalse("Expect concept to have definition in at least one language", definitionResource.listProperties(RDFS.label).toList().isEmpty())
        assertTrue("Expect concept to have non-empty translations of definition", definitionResource.listProperties(RDFS.label).toList().stream().map { it.literal }.map { it.string }.filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
        assertTrue("Expect concept definition to be translated to valid languages", validLanguageCodes.containsAll(definitionLanguages))

        if (definitionResource.hasProperty(SKOSNO.omfang)) {
            val definitionScopeResource = definitionResource.getProperty(SKOSNO.omfang).resource
            val definitionScopeLanguages = definitionScopeResource.listProperties(RDFS.label).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

            assertNotNull("Expect concept definition to have a scope", definitionScopeResource)
            assertTrue("Expect concept definition scope to have a label", definitionScopeResource.hasProperty(RDFS.label))
            assertFalse("Expect concept definition scope label in at least one language", definitionScopeResource.listProperties(RDFS.label).toList().isEmpty())
            assertTrue("Expect concept definition to have non-empty translations of scope", prefLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.literal }.map { it.string }.filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept definition scope to be translated to valid languages", validLanguageCodes.containsAll(definitionScopeLanguages))
            assertTrue("Expect concept definition scope to have a reference link", definitionScopeResource.hasProperty(RDFS.seeAlso))
            assertTrue("Expect concept definition scope reference link to be a URI resource", definitionScopeResource.getProperty(RDFS.seeAlso).resource.isURIResource)
            assertFalse("Expect concept definition scope reference link to be a URI not to be empty", definitionScopeResource.getProperty(RDFS.seeAlso).resource.uri.isEmpty())
        } else {
            val scope = concept.omfang
            if (scope != null && (scope.tekst.trim().isNotEmpty() || scope.uri.trim().isNotEmpty())) {
                assertTrue("Expect concept definition to have a scope", definitionResource.hasProperty(SKOSNO.omfang))
            }
        }

        if (definitionResource.hasProperty(SKOS.scopeNote)) {
            val definitionScopeNotesResources = definitionResource.listProperties(SKOS.scopeNote).toList().stream().map { it.string }.collect(Collectors.toList())
            val definitionScopeNoteLanguages = definitionResource.listProperties(SKOS.scopeNote).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

            assertFalse("Expect concept definition to have at least one scope note", definitionScopeNotesResources.isEmpty())
            assertTrue("Expect concept definition to have non-empty translations of scope notes", definitionScopeNotesResources.stream().filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept definition scope notes to be translated to valid languages", validLanguageCodes.containsAll(definitionScopeNoteLanguages))
        } else {
            val scopeNote = concept.merknad
            if (scopeNote != null && scopeNote.isNotEmpty()) {
                assertTrue("Expect concept definition to have a scope note", definitionResource.hasProperty(SKOS.scopeNote))
            }
        }

        if (definitionResource.hasProperty(SKOSNO.forholdTilKilde)) {
            assertEquals("Expect concept definition to have only one source type", 1, definitionResource.listProperties(SKOSNO.forholdTilKilde).toList().size)
            assertTrue("Expect source type to be of allowed type", definitionResource.listProperties(SKOSNO.forholdTilKilde).toList().stream().map { it.`object` }.allMatch { sourceType -> sourceType.toString() == SourceType.Source.Userdefined.toString() || sourceType.toString() == SourceType.Source.BasedOn.toString() || sourceType.toString() == SourceType.Source.QuoteFrom.toString() })
        } else {
            concept.kildebeskrivelse?.let {
                it.forholdTilKilde?.let {
                    assertTrue("Expect concept definition to have a source type", definitionResource.hasProperty(SKOSNO.forholdTilKilde))
                }
            }
        }

        if (definitionResource.hasProperty(DCTerms.source)) {
            val sourcesResources = definitionResource.listProperties(DCTerms.source).toList().stream().map { it.resource }.collect(Collectors.toList())

            sourcesResources.forEach {
                assertTrue("Expect concept definition source element to have the correct structure", it.hasProperty(RDFS.label) && it.hasProperty(RDFS.seeAlso))
                assertTrue("Expect concept definition source link to be a URI resource", it.getProperty(RDFS.seeAlso).resource.isURIResource)
                assertFalse("Expect concept definition source link not to be empty", it.getProperty(RDFS.seeAlso).resource.uri.trim().isEmpty())
                assertTrue("Expect concept definition source link labels to be translated to valid languages", validLanguageCodes.containsAll(it.listProperties(RDFS.label).toList().stream().map { st -> st.language }.distinct().collect(Collectors.toList())))
            }
        } else {
            concept.kildebeskrivelse?.let {
                if (it.kilde != null && it.kilde.isNotEmpty()) {
                    assertTrue("Expect concept definition to have sources", definitionResource.hasProperty(DCTerms.source))
                }
            }
        }

        if (conceptResource.hasProperty(SKOSXL.altLabel)) {
            val altLabelResource = conceptResource.getProperty(SKOSXL.altLabel).resource
            val altLabelLanguages = altLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

            assertTrue("Expect concept alternative label to have a literal form", altLabelResource.hasProperty(SKOSXL.literalForm))
            assertFalse("Expect concept to have alternative label in at least one language", altLabelResource.listProperties(SKOSXL.literalForm).toList().isEmpty())
            assertTrue("Expect concept to have non-empty translations of alternative label", altLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.string }.filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept alt labels to be translated to valid languages", validLanguageCodes.containsAll(altLabelLanguages))
        } else {
            val altLabel = concept.tillattTerm
            if (altLabel != null && altLabel.isNotEmpty()) {
                assertTrue("Expect concept to have alternative labels", conceptResource.hasProperty(SKOSXL.altLabel))
            }
        }

        if (conceptResource.hasProperty(SKOSXL.hiddenLabel)) {
            val hiddenLabelResource = conceptResource.getProperty(SKOSXL.hiddenLabel).resource
            val hiddenLabelLanguages = hiddenLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.language }.distinct().collect(Collectors.toList())

            assertTrue("Expect concept hidden label to have a literal form", hiddenLabelResource.hasProperty(SKOSXL.literalForm))
            assertFalse("Expect concept to have hidden label in at least one language", hiddenLabelResource.listProperties(SKOSXL.literalForm).toList().isEmpty())
            assertTrue("Expect concept to have non-empty translations of hidden label", hiddenLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.string }.filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept hidden labels to be translated to valid languages", validLanguageCodes.containsAll(hiddenLabelResource.listProperties(SKOSXL.literalForm).toList().stream().map { it.language }.collect(Collectors.toList())))
            assertTrue("Expect concept hidden labels to be translated to valid languages", validLanguageCodes.containsAll(hiddenLabelLanguages))
        } else {
            val hiddenLabel = concept.frarådetTerm
            if (hiddenLabel != null && hiddenLabel.isNotEmpty()) {
                assertTrue("Expect concept to have hidden labels", conceptResource.hasProperty(SKOSXL.hiddenLabel))
            }
        }

        if (conceptResource.hasProperty(SKOS.example)) {
            val conceptExamples = conceptResource.listProperties(SKOS.example).toList().stream().map { it.string }.collect(Collectors.toList())
            val conceptExampleLanguages = conceptResource.listProperties(SKOS.example).toList().stream().map { it.language }.collect(Collectors.toList())

            assertFalse("Expect concept to have at least one example", conceptExamples.isEmpty())
            assertTrue("Expect concept to have non-empty translations of examples", conceptExamples.stream().filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept examples to be translated to valid languages", validLanguageCodes.containsAll(conceptExampleLanguages))
        } else {
            val example = concept.eksempel
            if (example != null && example.isNotEmpty()) {
                assertTrue("Expect concept to have an example", conceptResource.hasProperty(SKOS.example))
            }
        }

        if (conceptResource.hasProperty(DCTerms.subject)) {
            val conceptSubjects = conceptResource.listProperties(DCTerms.subject).toList().stream().map { it.string }.collect(Collectors.toList())
            val conceptSubjectLanguages = conceptResource.listProperties(DCTerms.subject).toList().stream().map { it.language }.collect(Collectors.toList())

            assertFalse("Expect concept to have at least one subject", conceptSubjects.isEmpty())
            assertTrue("Expect concept to have non-empty translations of subjects", conceptSubjects.stream().filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept subjects to be translated to valid languages", validLanguageCodes.containsAll(conceptSubjectLanguages))
        } else {
            val subject = concept.fagområde
            if (subject != null && subject.isNotEmpty()) {
                assertTrue("Expect concept to have a subject", conceptResource.hasProperty(DCTerms.subject))
            }
        }

        if (conceptResource.hasProperty(SKOSNO.bruksområde)) {
            val conceptDomainsOfUse = conceptResource.listProperties(SKOSNO.bruksområde).toList().stream().map { it.string }.collect(Collectors.toList())
            val conceptDomainOfUseLanguages = conceptResource.listProperties(SKOSNO.bruksområde).toList().stream().map { it.language }.collect(Collectors.toList())

            assertFalse("Expect concept to have at least one domain of use", conceptDomainsOfUse.isEmpty())
            assertTrue("Expect concept to have non-empty translations of domains of use", conceptDomainsOfUse.stream().filter { Objects.nonNull(it) }.noneMatch { label -> label.trim().isEmpty() })
            assertTrue("Expect concept domains of use to be translated to valid languages", validLanguageCodes.containsAll(conceptDomainOfUseLanguages))
        } else {
            val domainOfUse = concept.bruksområde
            if (domainOfUse != null && domainOfUse.isNotEmpty()) {
                assertTrue("Expect concept to have domains of use", conceptResource.hasProperty(SKOSNO.bruksområde))
            }
        }

        if (conceptResource.hasProperty(DCAT.contactPoint)) {
            val contactPointResource = conceptResource.getProperty(DCAT.contactPoint).resource

            assertNotNull("Expect concept to have a contact point", contactPointResource)

            if (contactPointResource.hasProperty(VCARD4.hasEmail)) {
                val contactPointEmailResource = contactPointResource.getProperty(VCARD4.hasEmail).resource

                assertNotNull("Expect concept contact point to have an email", contactPointEmailResource)
                assertTrue("Expect concept contact point email to be a URI resource", contactPointEmailResource.isURIResource)
                assertFalse("Expect concept contact point email not to be empty", contactPointEmailResource.uri.trim().isEmpty())
                assertTrue("Expect concept contact point email value to have correct prefix", contactPointEmailResource.uri.startsWith("mailto:"))
            } else {
                val contactPoint = concept.kontaktpunkt
                if (contactPoint != null) {
                    val email = contactPoint.harEpost
                    if (email != null && email.trim().isNotEmpty()) {
                        assertTrue("Expect concept contact point to have an email", contactPointResource.hasProperty(VCARD4.hasEmail))
                    }
                }
            }

            if (contactPointResource.hasProperty(VCARD4.hasTelephone)) {
                val contactPointTelephoneResource = contactPointResource.getProperty(VCARD4.hasTelephone).resource

                assertNotNull("Expect concept contact point to have a telephone number", contactPointTelephoneResource)
                assertTrue("Expect concept contact point telephone number to be a URI resource", contactPointTelephoneResource.isURIResource)
                assertFalse("Expect concept contact point telephone number not to be empty", contactPointTelephoneResource.uri.trim().isEmpty())
                assertTrue("Expect concept contact point telephone number value to have correct prefix", contactPointTelephoneResource.uri.startsWith("tel:"))
            } else {
                val contactPoint = concept.kontaktpunkt
                if (contactPoint != null) {
                    val phone = contactPoint.harTelefon
                    if (phone != null && phone.trim().isNotEmpty()) {
                        assertTrue("Expect concept contact point to have a telephone number", contactPointResource.hasProperty(VCARD4.hasTelephone))
                    }
                }
            }
        } else {
            val contactPoint = concept.kontaktpunkt
            if (contactPoint != null) {
                assertTrue("Expect concept to have a contact point", conceptResource.hasProperty(DCAT.contactPoint))
            }
        }

        if (conceptResource.hasProperty(RDFS.seeAlso)) {
            val seeAlsoProperties = conceptResource.listProperties(RDFS.seeAlso).toList().stream().map { it.string }.collect(Collectors.toList())

            assertFalse("Expect concept to have at least one see also reference", seeAlsoProperties.isEmpty())
            assertTrue("Expect concept  see also references not to be empty", seeAlsoProperties.stream().allMatch { !it.isNullOrEmpty() })
        } else {
            val seeAlso = concept.seOgså
            if (seeAlso != null && seeAlso.isNotEmpty()) {
                assertTrue("Expect concept to have see also references", conceptResource.hasProperty(RDFS.seeAlso))
            }
        }
    }
}
