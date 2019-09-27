package no.begrepskatalog.conceptregistration

import com.nhaarman.mockitokotlin2.*
import no.begrepskatalog.conceptregistration.security.FdkPermissions
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.conceptregistration.utils.patchBegrep
import no.begrepskatalog.generated.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest


class ControllerTest {
    @Test
    fun test_generation_of_urls_for_new_begrep() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val httpServletRequestMock: HttpServletRequest = mock()
        val fdkPermissionsMock: FdkPermissions = prepareFdkPermissionsMock()

        val begreperApiImplK = BegreperApiImplK(sqlStoreMock, fdkPermissionsMock)
        begreperApiImplK.baseURL = "https://registrering-begrep.ut1.fellesdatakatalog.brreg.no"
        val begrep = Begrep()
        begrep.ansvarligVirksomhet= createTestVirksomhet()

        val retValue = begreperApiImplK.createBegrep(httpServletRequestMock, begrep)
        assertNotNull(retValue)
        assertNotNull(retValue.headers)
        assertEquals( begreperApiImplK.baseURL + makeBegrep().ansvarligVirksomhet.id + "/" + makeBegrep().id, retValue.headers.get("Location")?.get(0))

    }

    @Ignore // TODO: there is no functionality for on-demand validation
    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_does_not_exist() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val fdkPermissionsMock: FdkPermissions = prepareFdkPermissionsMock()

        val emptyBegrep = Begrep()

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock,fdkPermissionsMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", listOf(), true)
    }

    @Ignore // TODO: there is no functionality for on-demand validation
    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_has_not_ansvarligVirksomhet() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val fdkPermissionsMock: FdkPermissions = prepareFdkPermissionsMock()

        val someBegrep = makeBegrep()//Same as mock pretends to save
        someBegrep.ansvarligVirksomhet = null

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock, fdkPermissionsMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", listOf(), true)
    }

    @Test
    fun test_deletion_both_checks_existence_and_actually_calls_delete() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val fdkPermissionsMock: FdkPermissions = prepareFdkPermissionsMock()

        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(sqlStoreMock, fdkPermissionsMock)
        val retValue = begreperApiImplK.deleteBegrepById(httpServletRequestMock, "dummyId")

        verify(sqlStoreMock).deleteBegrepById("dummyId")
        verify(sqlStoreMock).begrepExists("dummyId")
    }

    @Test
    fun test_liveness_and_readyness() {
        val sqlStoreMock: SqlStore = prepareSqlStoreMock()
        val fdkPermissionsMock: FdkPermissions = prepareFdkPermissionsMock()

        val begreperApiImplK = BegreperApiImplK(sqlStoreMock, fdkPermissionsMock)
        assert(begreperApiImplK.ping().statusCode == HttpStatus.OK)
        assert(begreperApiImplK.ready().statusCode == HttpStatus.OK)
    }

    @Test
    fun test_patch_begrep() {
        val source = makeDataFilledBegrepA()
        val sourceToTestAgainst = makeDataFilledBegrepB()

        val patchedBegrep = patchBegrep(source, createChanges())

        //verify that destination has all values ending in A (so we know that they are copied over)
        assert(patchedBegrep.status == sourceToTestAgainst.status)
        assert(patchedBegrep.definisjon == sourceToTestAgainst.definisjon)
        assert(patchedBegrep.anbefaltTerm == sourceToTestAgainst.anbefaltTerm)
        assert(patchedBegrep.definisjon == sourceToTestAgainst.definisjon)
        assert(patchedBegrep.kildebeskrivelse.forholdTilKilde == sourceToTestAgainst.kildebeskrivelse.forholdTilKilde)
        assert(patchedBegrep.kildebeskrivelse.kilde.size == sourceToTestAgainst.kildebeskrivelse.kilde.size)
        assert(patchedBegrep.merknad == sourceToTestAgainst.merknad)
        assert(patchedBegrep.fagområde == sourceToTestAgainst.fagområde)
        assert(patchedBegrep.bruksområde == sourceToTestAgainst.bruksområde)
        assert(patchedBegrep.kontaktpunkt.harTelefon == sourceToTestAgainst.kontaktpunkt.harTelefon)
        assert(patchedBegrep.kontaktpunkt.harEpost == sourceToTestAgainst.kontaktpunkt.harEpost)
        assert(patchedBegrep.omfang.tekst == sourceToTestAgainst.omfang.tekst)
        assert(patchedBegrep.omfang.uri == sourceToTestAgainst.omfang.uri)
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
                bruksområde = listOf("medisinA")
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
                bruksområde = listOf("medisinB")
                omfang = omfangB()
                kontaktpunkt = pkunktB()
                gyldigFom = LocalDate.now().plusMonths(1)
                endringslogelement = Endringslogelement()
                endringslogelement.brukerId = "brukerIdB"
                endringslogelement.endringstidspunkt = OffsetDateTime.now().plusMonths(1)
            }

    fun createChanges(): List<JsonPatchOperation> {
        return listOf(
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/definisjon"
                    value = "testbegrepB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/anbefaltTerm"
                    value = "fødestedB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/definisjon"
                    value = "er geografisk navn på hvor i fødekommunen eller fødelandet personen er født.B"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/kildebeskrivelse/forholdTilKilde"
                    value = Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/kildebeskrivelse/kilde/0/tekst"
                    value = "skatteetatenB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/kildebeskrivelse/kilde/0/uri"
                    value = "www.skatteetaten.noB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/merknad"
                    value = "testbegrepB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/eksempel"
                    value = "bergenB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/fagområde"
                    value = "fødeB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/bruksområde/0"
                    value = "medisinB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/omfang/tekst"
                    value = "sometextB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/omfang/uri"
                    value = "http://someuri.comB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/kontaktpunkt/harEpost"
                    value = "somebody@somewhere.comB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/kontaktpunkt/harTelefon"
                    value = "55555555B"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/gyldigFom"
                    value = LocalDate.now().plusMonths(1).toString()
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/endringslogelement/brukerId"
                    value = "brukerIdB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/endringslogelement/endringstidspunkt"
                    value = OffsetDateTime.now().plusMonths(1).toString()
                }
        )
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
        val sqlStoreMock: SqlStore = mock {}


        whenever(sqlStoreMock.getBegrepById("dummyId")).thenReturn(makeBegrep())
        whenever(sqlStoreMock.begrepExists("dummyId")).thenReturn(true)
        whenever(sqlStoreMock.begrepExists("NonExistingId")).thenReturn(false)
        whenever(sqlStoreMock.ready()).thenReturn(true)
        whenever(sqlStoreMock.saveBegrep(any())).thenReturn(makeBegrep())

        return sqlStoreMock
    }

    private fun prepareFdkPermissionsMock(): FdkPermissions {
        val fdkPermissionsMock: FdkPermissions = mock {}

        whenever(fdkPermissionsMock.hasPermission(any(),any(),any())).thenReturn(true)

        return fdkPermissionsMock
    }
}