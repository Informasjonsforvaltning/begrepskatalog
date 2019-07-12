package no.begrepskatalog.conceptregistration

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.servlet.http.HttpServletRequest


class ControllerTest {

    @Test
    fun test_generation_of_urls_for_new_begrep() {
        val sqlStoreMock: SqlStore = mock {
            on {
                saveBegrep(Begrep())
            } doReturn makeBegrep()
        }
        val httpServletRequestMock: HttpServletRequest = mock()

        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        begreperApiImplK.baseURL = "https://registrering-begrep.ut1.fellesdatakatalog.brreg.no"
        val begrep = Begrep()

        val retValue = begreperApiImplK.createBegrep(httpServletRequestMock, begrep)
        assertNotNull(retValue)
        assertNotNull(retValue.headers)
        assertEquals(retValue.headers.get("Location")?.get(0), begreperApiImplK.baseURL + "/" + makeBegrep().ansvarligVirksomhet.id + "/" + makeBegrep().id)

    }

    fun makeBegrep(): Begrep {

        val begrep = Begrep()
        begrep.id = "1c770979-34b0-439c-a7cb-adacb3619927"
        begrep.definisjon = "testbegrep"
        begrep.ansvarligVirksomhet = createTestVirksomhet()
        return begrep
    }

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
}