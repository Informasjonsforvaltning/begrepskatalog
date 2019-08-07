package no.begrepskatalog.conceptregistration

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest


class ControllerTest {

    @Test
    fun test_generation_of_urls_for_new_begrep() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val httpServletRequestMock: HttpServletRequest = mock()

        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        begreperApiImplK.baseURL = "https://registrering-begrep.ut1.fellesdatakatalog.brreg.no"
        val begrep = Begrep()

        val retValue = begreperApiImplK.createBegrep(httpServletRequestMock, begrep)
        assertNotNull(retValue)
        assertNotNull(retValue.headers)
        assertEquals(retValue.headers.get("Location")?.get(0), begreperApiImplK.baseURL + makeBegrep().ansvarligVirksomhet.id + "/" + makeBegrep().id)

    }

    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_does_not_exist() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()

        val emptyBegrep = Begrep()

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", emptyBegrep, true)
    }

    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_has_not_ansvarligVirksomhet() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()

        val someBegrep = makeBegrep()//Same as mock pretends to save
        someBegrep.ansvarligVirksomhet = null

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", someBegrep, true)
    }

    @Test
    fun test_deletion_both_checks_existence_and_actually_calls_delete() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        val retValue = begreperApiImplK.deleteBegrepById(httpServletRequestMock, "dummyId")

        verify(sqlStoreMock).deleteBegrepById("dummyId")
        verify(sqlStoreMock).begrepExists("dummyId")
    }

    @Test
    fun test_update_begrep() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        val source = makeDataFilledBegrepA()
        val sourceToTestAgainst = makeDataFilledBegrepA()
        val destination = makeDataFilledBegrepB()

        val updatedBegrep = begreperApiImplK.updateBegrep(source, destination)

        //verify that destination has all values ending in A (so we know that they are copied over)
        assert(updatedBegrep.status == sourceToTestAgainst.status)
        assert(updatedBegrep.definisjon == sourceToTestAgainst.definisjon)
        assert(updatedBegrep.anbefaltTerm == sourceToTestAgainst.anbefaltTerm)
        assert(updatedBegrep.definisjon == sourceToTestAgainst.definisjon)
        assert(updatedBegrep.kildebeskrivelse.forholdTilKilde == sourceToTestAgainst.kildebeskrivelse.forholdTilKilde)
        assert(updatedBegrep.kildebeskrivelse.kilde.size == sourceToTestAgainst.kildebeskrivelse.kilde.size)
        assert(updatedBegrep.merknad == sourceToTestAgainst.merknad)
        assert(updatedBegrep.fagområde == sourceToTestAgainst.fagområde)
        assert(updatedBegrep.bruksområde == sourceToTestAgainst.bruksområde)
        assert(updatedBegrep.kontaktpunkt.harTelefon == sourceToTestAgainst.kontaktpunkt.harTelefon)
        assert(updatedBegrep.kontaktpunkt.harEpost == sourceToTestAgainst.kontaktpunkt.harEpost)
        assert(updatedBegrep.omfang.tekst == sourceToTestAgainst.omfang.tekst)
        assert(updatedBegrep.omfang.uri == sourceToTestAgainst.omfang.uri)
    }


    fun makeBegrep(): Begrep =
            Begrep().apply {
                id = "1c770979-34b0-439c-a7cb-adacb3619927"
                definisjon = "testbegrep"
                ansvarligVirksomhet = createTestVirksomhet()
            }

    fun makeDataFilledBegrepA(): Begrep =
            Begrep().apply {
                id = "1c770979-34b0-439c-a7cb-adacb3619927"
                definisjon = "testbegrepA"
                ansvarligVirksomhet = createTestVirksomhet()
                status = Status.UTKAST
                anbefaltTerm = "fødestedA"
                definisjon = "er geografisk navn på hvor i fødekommunen eller fødelandet personen er født.A"
                kildebeskrivelse = Kildebeskrivelse()
                kildebeskrivelse.kilde = mutableListOf()
                val kilde = URITekst()
                kilde.tekst = "skatteetatenA"
                kilde.uri = "www.skatteetaten.noA"
                kildebeskrivelse.kilde.add(kilde)
                kildebeskrivelse.forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE
                merknad = "testbegrepA"
                eksempel = "bergenA"
                fagområde = "fødeA"
                bruksområde = "medisinA"
                omfang = omfangA()
                kontaktpunkt = pkunktA()
                gyldigFom = LocalDate.now()
                endringslogelement = Endringslogelement()
                endringslogelement.brukerId = "brukerIdA"
                endringslogelement.endringstidspunkt = OffsetDateTime.now()
            }

    fun makeDataFilledBegrepB(): Begrep =
            Begrep().apply {
                id = "1c770979-34b0-439c-a7cb-adacb3619927"
                definisjon = "testbegrepB"
                ansvarligVirksomhet = createTestVirksomhet()
                status = Status.UTKAST
                anbefaltTerm = "fødestedB"
                definisjon = "er geografisk navn på hvor i fødekommunen eller fødelandet personen er født.B"
                kildebeskrivelse = Kildebeskrivelse()
                val kilde = URITekst()
                kilde.tekst = "skatteetatenB"
                kilde.uri = "www.skatteetaten.noB"
                kildebeskrivelse.kilde.add(kilde)
                kildebeskrivelse.forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT
                merknad = "testbegrepB"
                eksempel = "bergenB"
                fagområde = "fødeB"
                bruksområde = "medisinB"
                omfang = omfangB()
                kontaktpunkt = pkunktB()
                gyldigFom = LocalDate.now().plusMonths(1)
                endringslogelement = Endringslogelement()
                endringslogelement.brukerId = "brukerIdB"
                endringslogelement.endringstidspunkt = OffsetDateTime.now().plusMonths(1)
            }

    private fun omfangA(): URITekst {
        val omf = URITekst().apply {
            tekst = "sometextA"
            uri = "http://someuri.comA"
        }
        return omf
    }

    private fun omfangB(): URITekst {
        val omf = URITekst().apply {
            tekst = "sometextB"
            uri = "http://someuri.comB"
        }
        return omf
    }

    private fun pkunktA(): Kontaktpunkt {
        val kpunkt = Kontaktpunkt().apply {
            harEpost = "somebody@somewhere.comA"
            harTelefon = "55555555A"
        }
        return kpunkt
    }

    private fun pkunktB(): Kontaktpunkt {
        val kpunkt = Kontaktpunkt().apply {
            harEpost = "somebody@somewhere.comB"
            harTelefon = "55555555B"
        }
        return kpunkt
    }

    fun createTestVirksomhet(): no.begrepskatalog.generated.model.Virksomhet =
            Virksomhet().apply {
                id = "910244132"
                navn = "Ramsund og Rognand revisjon"
                orgPath = "/helt/feil/dummy/path"
                prefLabel = "preflabel"
                uri = "ramsumdURI"
            }

    private fun prepareSqlStoreMock(): SqlStore {
        val sqlStoreMock: SqlStore = mock {
            on {
                saveBegrep(Begrep())
            } doReturn makeBegrep()
        }
        whenever(sqlStoreMock.begrepExists("dummyId")).thenReturn(true)
        whenever(sqlStoreMock.begrepExists("NonExistingId")).thenReturn(false)

        return sqlStoreMock
    }
}