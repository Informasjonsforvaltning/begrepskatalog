package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
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
            bruksområde = "Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer"
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
}
