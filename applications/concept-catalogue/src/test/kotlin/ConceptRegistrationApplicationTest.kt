package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Test
import org.junit.Ignore
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate

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

    @Test
    fun contextLoads() {
    }

    @Test
    fun buildSmallSetOfTestData() {
        logger.info("Building test data!")

        //Note that a company can have many begrep, but we do not want to create more than 1 in this test
        val begreps = sqlStore.getBegrepByCompany("910244132")

        if (begreps.isEmpty()) {
            val testBegrep = Begrep().apply {
                anbefaltTerm = "eplesaft"
                id = java.util.UUID.randomUUID().toString()
                ansvarligVirksomhet = createTestVirksomhet()
                bruksområde = "Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer"
                definisjon = "saft uten tilsatt sukker som er basert på epler"
                eksempel = "DummyEksempel"
                status = Status.UTKAST
                gyldigFom = LocalDate.now()
            }

            sqlStore.saveBegrep(testBegrep)
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
}
