package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.conceptregistration.validation.isValidBegrep
import no.begrepskatalog.generated.model.*
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest
class ConceptRegistrationApplicationTests {

    @Autowired
    lateinit var sqlStore: SqlStore

    private val logger = LoggerFactory.getLogger(ConceptRegistrationApplicationTests::class.java)

    fun createTestVirksomhet() : no.begrepskatalog.generated.model.Virksomhet {

        val testVirksomhet = Virksomhet().apply {
            id = "910244132"
            navn = "Ramsund og Rognand revisjon"
            orgPath = "/helt/feil/dummy/path"
            prefLabel =  "preflabel"
            uri = "ramsumdURI"
        }
        return testVirksomhet
    }

    @Ignore
    @Test
    fun contextLoads() {
    }

    @Ignore
    @Test
    fun buildSmallSetOfTestData() {
        logger.info("Building test data!")

        //Note that a company can have many begrep, but we do not want to create more than 1 in this test
        val begreps = sqlStore.getBegrepByCompany("910244132")

        if (begreps.isEmpty()) {
            val testBegrep = createBegrep()
            sqlStore.saveBegrep(testBegrep)
        }
    }

    private fun createBegrep(): Begrep {
        val testBegrep = Begrep().apply {
            anbefaltTerm = "eplesaft"
            id = UUID.randomUUID().toString()
            ansvarligVirksomhet = createTestVirksomhet()
            bruksområde = "Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer"
            definisjon = "saft uten tilsatt sukker som er basert på epler"
            eksempel = "DummyEksempel"
            status = Status.UTKAST
            gyldigFom = LocalDate.now()
        }
        return testBegrep
    }
    @Ignore
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

    @Ignore
    @Test
    fun savePublishedBegrep() {
        var testBegrep = createBegrep()
        testBegrep.status = Status.PUBLISERT
        sqlStore.saveBegrep(testBegrep)
    }
    @Ignore
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
    @Ignore
    @Test
    fun testExistsNotFalsePositive() {
        val emptyBegrep = Begrep().apply {
            status = Status.UTKAST
            id = UUID.randomUUID().toString()
        }
        assertFalse(sqlStore.begrepExists(emptyBegrep))
    }

    @Ignore
    @Test
    fun testCreateAndDelete() {
        val begrep = createBegrep()
        sqlStore.saveBegrep(begrep)
        assertTrue(sqlStore.begrepExists(begrep))

        sqlStore.deleteBegrepById(begrep.id)

        assertFalse(sqlStore.begrepExists(begrep))
    }

    @Test
    fun testBegrepStatusMustNotBeUtkastDuringValidation() {
        val begrep = createBegrep()

        begrep.status = Status.PUBLISERT
        assertTrue(isValidBegrep(begrep))

        begrep.status = Status.GODKJENT
        assertTrue(isValidBegrep(begrep))

        begrep.status = Status.UTKAST
        assertFalse(isValidBegrep(begrep))

        begrep.status = null
        assertFalse(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepAnbefaltTermMustBeSetAndNotEmpty() {
        val begrep = createBegrep()
        begrep.status = Status.GODKJENT

        begrep.anbefaltTerm = null
        assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = ""
        assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = "   "
        assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = "anbefaltTerm"
        assertTrue(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepDefinisjonValidationMustBeSetAndNotEmpty() {
        val begrep = createBegrep()
        begrep.status = Status.GODKJENT

        begrep.definisjon = null
        assertFalse(isValidBegrep(begrep))

        begrep.definisjon = ""
        assertFalse(isValidBegrep(begrep))

        begrep.definisjon = "   "
        assertFalse(isValidBegrep(begrep))

        begrep.definisjon = "definisjon"
        assertTrue(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepAnsvarligVirksomhetMustHaveAValidOrganisationNumber() {
        val begrep = createBegrep()
        begrep.status = Status.GODKJENT

        begrep.ansvarligVirksomhet = null
        assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = null
        }
        assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = ""
        }
        assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "123"
        }
        assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "123456789"
        }
        assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet()
        assertTrue(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "943574537"
        }
        assertTrue(isValidBegrep(begrep))
    }
}
