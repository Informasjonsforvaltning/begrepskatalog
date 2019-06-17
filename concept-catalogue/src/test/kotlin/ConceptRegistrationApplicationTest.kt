package no.begrepskatalog.conceptregistration

import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Test
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


    @Test
    fun contextLoads() {
    }

    @Test
    fun buildSmallSetOfTestData() {
        logger.info("Building test data!")

        val testVirksomhet = Virksomhet()
        testVirksomhet.id = "910244132"
        testVirksomhet.navn = "Ramsund og Rognand revisjon"
        testVirksomhet.orgPath = "/helt/feil/dummy/path"
        testVirksomhet.prefLabel = "preflabel"
        testVirksomhet.uri = "ramsumdURI"

        //Note that a company can have many begrep, but we do not want to create more than 1 in this test
        val begreps = sqlStore.getBegrepByCompany("910244132")

        if (begreps.isEmpty()) {
            val testBegrep = Begrep()
            testBegrep.anbefaltTerm = "eplesaft"
            testBegrep.id = java.util.UUID.randomUUID().toString()
            testBegrep.ansvarligVirksomhet = testVirksomhet
            testBegrep.bruksområde = "Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer"
            testBegrep.definisjon = "saft uten tilsatt sukker som er basert på epler"
            testBegrep.eksempel = "DummyEksempel"
            testBegrep.id = "511ffd3b-b833-4f12-941d-8aa092265a18"
            testBegrep.status = Status.UTKAST
            testBegrep.gyldigFom = LocalDate.now()

            sqlStore.saveBegrep(testBegrep)
        }
    }
}
