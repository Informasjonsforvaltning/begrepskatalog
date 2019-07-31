package no.begrepskatalog.conceptregistration

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.servlet.http.HttpServletRequest
import java.time.LocalDate


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
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock,"esf3",emptyBegrep,true)
    }

    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_has_not_ansvarligVirksomhet() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()

        val someBegrep = makeBegrep()//Same as mock pretends to save
        someBegrep.ansvarligVirksomhet = null

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock,"esf3",someBegrep,true)
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
        val destination  = makeDataFilledBegrepB()

        val updatedBegrep = begreperApiImplK.updateBegrep(source, destination)

        //verify that destination has all values ending in A (so we know that they are copied over)
        assert(updatedBegrep.status == sourceToTestAgainst.status )
        assert(updatedBegrep.definisjon == sourceToTestAgainst.definisjon )
        assert(updatedBegrep.anbefaltTerm == sourceToTestAgainst.anbefaltTerm )
        assert(updatedBegrep.definisjon == sourceToTestAgainst.definisjon )
        assert(updatedBegrep.kilde == sourceToTestAgainst.kilde )
        assert(updatedBegrep.merknad == sourceToTestAgainst.merknad )
        assert(updatedBegrep.fagområde == sourceToTestAgainst.fagområde )
        assert(updatedBegrep.bruksområde == sourceToTestAgainst.bruksområde )
        assert(updatedBegrep.verdiområde == sourceToTestAgainst.verdiområde )
        assert(updatedBegrep.kontaktpunkt == sourceToTestAgainst.kontaktpunkt )
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
                kilde = "skatteetatenA"
                merknad = "testbegrepA"
                eksempel = "bergenA"
                fagområde = "fødeA"
                bruksområde = "medisinA"
                verdiområde = "geografiskA"
                kontaktpunkt = "55555555A"
                gyldigFom = LocalDate.now()
            }

    fun makeDataFilledBegrepB(): Begrep =
            Begrep().apply {
                id = "1c770979-34b0-439c-a7cb-adacb3619927"
                definisjon = "testbegrepB"
                ansvarligVirksomhet = createTestVirksomhet()
                status = Status.UTKAST
                anbefaltTerm = "fødestedB"
                definisjon = "er geografisk navn på hvor i fødekommunen eller fødelandet personen er født.B"
                kilde = "skatteetatenB"
                merknad = "testbegrepB"
                eksempel = "bergenB"
                fagområde = "fødeB"
                bruksområde = "medisinB"
                verdiområde = "geografiskB"
                kontaktpunkt = "55555555B"
                gyldigFom = LocalDate.now().plusMonths(1)
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