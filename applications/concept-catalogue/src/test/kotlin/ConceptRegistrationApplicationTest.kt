package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Kildebeskrivelse
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.util.*

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(initializers = [ConceptRegistrationApplicationTests.Initializer::class])
class ConceptRegistrationApplicationTests {

    @Autowired
    lateinit var sqlStore: SqlStore

    private val logger = LoggerFactory.getLogger(ConceptRegistrationApplicationTests::class.java)

    // Hack needed because testcontainers use of generics confuses Kotlin
    // More info at: https://blog.producement.com/tech/spring-boot/test/postgres/kotlin/2019/03/28/docker-postgres-test.html
    class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

    companion object {
        private val postgreSQLContainer: KPostgreSQLContainer by lazy {
            KPostgreSQLContainer("postgres:11.2")
                    .withUsername("testuser")
                    .withPassword("testpassword")
        }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            postgreSQLContainer.start()
            TestPropertyValues.of(
                    "spring.datasource.url=" + "${postgreSQLContainer.jdbcUrl}&currentSchema=conceptregistration",
                    "spring.datasource.username=" + postgreSQLContainer.username,
                    "spring.datasource.password=" + postgreSQLContainer.password
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    private fun createBegrep(): Begrep {
        return Begrep().apply {
            anbefaltTerm = "eplesaft"
            id = UUID.randomUUID().toString()
            ansvarligVirksomhet = createTestVirksomhet()
            bruksområde = listOf("Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer")
            definisjon = "saft uten tilsatt sukker som er basert på epler"
            eksempel = "DummyEksempel"
            status = Status.UTKAST
            gyldigFom = LocalDate.now()
        }
    }

    private fun createTestVirksomhet() : Virksomhet {
        return Virksomhet().apply {
            id = "910244132"
            navn = "Ramsund og Rognand revisjon"
            orgPath = "/helt/feil/dummy/path"
            prefLabel =  "preflabel"
            uri = "ramsumdURI"
        }
    }

    @Test
    fun saveEmptyBegrepToTestCreationOfId() {
        val emptyBegrep = Begrep().apply {
            status = Status.UTKAST
            ansvarligVirksomhet = createTestVirksomhet()
        }
        val savedBegrep = sqlStore.saveBegrep(emptyBegrep)
        assertNotNull(savedBegrep)
        assertNotNull(savedBegrep?.id)
    }

    @Test
    fun savePublishedBegrep() {
        var testBegrep = createBegrep()
        testBegrep.status = Status.PUBLISERT
        sqlStore.saveBegrep(testBegrep)
    }

    @Test
    fun testThatExistsWork() {
        val emptyBegrep = Begrep().apply {
            status = Status.UTKAST
            ansvarligVirksomhet = createTestVirksomhet()
        }
        val savedBegrep = sqlStore.saveBegrep(emptyBegrep)
        if (savedBegrep != null) {
            assertEquals(true, sqlStore.begrepExists(savedBegrep))
        } else {
            fail()
        }
    }

    @Test
    fun testExistsNotFalsePositive() {
        val emptyBegrep = Begrep().apply {
            status = Status.UTKAST
            id = UUID.randomUUID().toString()
        }
        assertFalse(sqlStore.begrepExists(emptyBegrep))
    }

    @Test
    fun testCreateAndDelete() {
        val begrep = createBegrep()
        begrep.id = null

        sqlStore.saveBegrep(begrep)
        assertTrue(sqlStore.begrepExists(begrep))

        sqlStore.deleteBegrepById(begrep.id)

        assertFalse(sqlStore.begrepExists(begrep))
    }

    @Test
    fun testCreateBegrepWithTillattTerm() {
        val begrep = createBegrep()
        begrep.id = null
        val tillattTerm = listOf("a", "b", "c")

        begrep.tillattTerm = tillattTerm

        val savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertArrayEquals(tillattTerm.toTypedArray(), retrievedBegrep!!.tillattTerm.toTypedArray())
    }

    @Test
    fun testCreateBegrepWithFrarådetTerm() {
        val begrep = createBegrep()
        begrep.id = null
        val frarådetTerm = listOf("a", "b", "c")

        begrep.frarådetTerm = frarådetTerm

        val savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertArrayEquals(frarådetTerm.toTypedArray(), retrievedBegrep!!.frarådetTerm.toTypedArray())
    }


    @Test
    fun testUpdateBegrepWithTillattTerm() {
        val begrep = createBegrep()
        begrep.id = null
        val tillattTermInitial = listOf("a", "b", "c")
        val tillattTermUpdated = listOf("a", "b", "c", "d")

        begrep.tillattTerm = tillattTermInitial

        val savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertArrayEquals(tillattTermInitial.toTypedArray(), retrievedBegrep!!.tillattTerm.toTypedArray())

        retrievedBegrep.tillattTerm = tillattTermUpdated
        sqlStore.saveBegrep(retrievedBegrep)

        val updatedBegrep = sqlStore.getBegrepById(retrievedBegrep!!.id)
        assertNotNull(updatedBegrep)
        assertArrayEquals(tillattTermUpdated.toTypedArray(), updatedBegrep!!.tillattTerm.toTypedArray())
    }

    @Test
    fun testUpdateBegrepWithFrarådetTerm() {
        val begrep = createBegrep()
        begrep.id = null
        val frarådetTermInitial = listOf("a", "b", "c")
        val frarådetTermUpdated = listOf("a", "b", "c", "d")

        begrep.frarådetTerm = frarådetTermInitial

        val savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertArrayEquals(frarådetTermInitial.toTypedArray(), retrievedBegrep!!.frarådetTerm.toTypedArray())

        retrievedBegrep.frarådetTerm = frarådetTermUpdated
        sqlStore.saveBegrep(retrievedBegrep)

        val updatedBegrep = sqlStore.getBegrepById(retrievedBegrep!!.id)
        assertNotNull(updatedBegrep)
        assertArrayEquals(frarådetTermUpdated.toTypedArray(), updatedBegrep!!.frarådetTerm.toTypedArray())
    }

    @Test
    fun testEnsureNullBruksområdeDoesNotThrowErrors() {
        val begrep = createBegrep()
        begrep.id = null
        begrep.bruksområde = null
        begrep.tillattTerm = null
        begrep.frarådetTerm = null

        val savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertTrue(retrievedBegrep!!.bruksområde.isEmpty())
        assertTrue(retrievedBegrep.tillattTerm.isEmpty())
        assertTrue(retrievedBegrep.frarådetTerm.isEmpty())
    }

    @Test
    fun testEnsureUnsetPropertiesDoNotGetOverwritten() {
        val bruksområde = listOf("a", "b", "c")
        val tillattTerm = listOf("d", "e", "f")
        val frarådetTerm = listOf("g", "h", "i")

        val begrep = createBegrep()
        begrep.id = null
        begrep.bruksområde = bruksområde
        begrep.tillattTerm = tillattTerm
        begrep.frarådetTerm = frarådetTerm

        var savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        savedBegrep!!.bruksområde = null

        savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))
        assertNull(savedBegrep?.bruksområde)
        assertArrayEquals(tillattTerm.toTypedArray(), savedBegrep!!.tillattTerm.toTypedArray())
        assertArrayEquals(frarådetTerm.toTypedArray(), savedBegrep!!.frarådetTerm.toTypedArray())

        savedBegrep!!.tillattTerm = null

        savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))
        assertNull(savedBegrep?.bruksområde)
        assertNull(savedBegrep?.tillattTerm)
        assertArrayEquals(frarådetTerm.toTypedArray(), savedBegrep!!.frarådetTerm.toTypedArray())

        savedBegrep!!.frarådetTerm = null

        savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))
        assertNull(savedBegrep?.bruksområde)
        assertNull(savedBegrep?.tillattTerm)
        assertNull(savedBegrep?.frarådetTerm)
    }

    @Test
    fun testEnsureKildebeskrivelseCanBeNull() {
        val begrep = createBegrep()
        begrep.id = null

        var savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        val retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertNull(retrievedBegrep?.kildebeskrivelse)
    }

    @Test
    fun testEnsureKildebeskrivelseIsNotNullWhenSet() {
        val begrep = createBegrep()
        begrep.id = null

        var savedBegrep = sqlStore.saveBegrep(begrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        var retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertNull(retrievedBegrep?.kildebeskrivelse)

        retrievedBegrep!!.kildebeskrivelse = Kildebeskrivelse().apply {
            forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT
            kilde = emptyList()
        }

        savedBegrep = sqlStore.saveBegrep(retrievedBegrep)
        assertNotNull(savedBegrep)
        assertTrue(sqlStore.begrepExists(begrep))

        retrievedBegrep = sqlStore.getBegrepById(savedBegrep!!.id)
        assertNotNull(retrievedBegrep)
        assertNotNull(retrievedBegrep!!.kildebeskrivelse)
        assertEquals(Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT, retrievedBegrep.kildebeskrivelse.forholdTilKilde)
    }
}
