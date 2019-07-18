package no.begrepskatalog.conceptregistration

import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class HarvestEndpointTest {

    @Autowired
    lateinit var harvestEndpoint: HarvestEndpoint

    fun createTestVirksomhet(): no.begrepskatalog.generated.model.Virksomhet {

        val testVirksomhet = Virksomhet().apply {
            id = "910244132"
            navn = "Ramsund og Rognand revisjon"
            orgPath = "/helt/feil/dummy/path"
            prefLabel = "preflabel"
            uri = "ramsumdURI"
        }
        return testVirksomhet
    }
    @Ignore
    @Test
    fun testHarvesting() {
        val someResponse = harvestEndpoint.harvest(null)
        assertNotNull(someResponse)
    }
    @Ignore
    @Test
    fun contextLoads() {
    }
}