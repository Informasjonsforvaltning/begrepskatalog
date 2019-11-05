package no.brreg.conceptcatalogue

import com.nhaarman.mockitokotlin2.*
import no.begrepskatalog.generated.model.*
import no.brreg.conceptcatalogue.repository.BegrepRepository
import no.brreg.conceptcatalogue.service.permission.PermissionService
import no.brreg.conceptcatalogue.service.ConceptPublisher
import no.brreg.conceptcatalogue.utils.patchBegrep
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest


class ControllerTest {
    @Test
    fun test_generation_of_urls_for_new_begrep() {
        val begrepRepositoryMock: BegrepRepository = prepareBegrepRepositoryMock()
        val httpServletRequestMock: HttpServletRequest = mock()
        val postRequestUri = "/begreper"
        whenever(httpServletRequestMock.requestURI).thenReturn(postRequestUri)

        val permissionServiceMock: PermissionService = prepareFdkPermissionsMock()
        val rabbitPublisherMock: ConceptPublisher = prepareRabbitMock()

        val begreperApiImplK = BegreperApiImplK(begrepRepositoryMock, permissionServiceMock, rabbitPublisherMock)
        begreperApiImplK.baseURL = "https://registrering-begrep.ut1.fellesdatakatalog.brreg.no"
        val begrep = Begrep()
        begrep.ansvarligVirksomhet = createTestVirksomhet()

        val retValue = begreperApiImplK.createBegrep(httpServletRequestMock, begrep)
        assertNotNull(retValue)
        assertNotNull(retValue.headers)
        val locationHeaderValue: String? = retValue.headers.get("Location")?.get(0);

        // NOTE: below regular expression is used to validate location header pattern, which has to be in the format (/begreper/<UUID>)
        val testRegExp = Regex("^/begreper/[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$", RegexOption.IGNORE_CASE);
        assertTrue("Incorrect location header value", locationHeaderValue?.matches(testRegExp) ?: false)
    }

    @Ignore // TODO: there is no functionality for on-demand validation
    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_does_not_exist() {
        val begrepRepositoryMock: BegrepRepository = prepareBegrepRepositoryMock()
        val permissionServiceMock: PermissionService = prepareFdkPermissionsMock()

        val emptyBegrep = Begrep()

        val rabbitPublisherMock: ConceptPublisher = prepareRabbitMock()
        val httpServletRequestMock: HttpServletRequest = mock()
        val begreperApiImplK = BegreperApiImplK(begrepRepositoryMock, permissionServiceMock, rabbitPublisherMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", listOf(), true)
    }

    @Ignore // TODO: there is no functionality for on-demand validation
    @Test(expected = RuntimeException::class)
    fun test_validations_begrep_has_not_ansvarligVirksomhet() {
        val begrepRepositoryMock: BegrepRepository = prepareBegrepRepositoryMock()
        val permissionServiceMock: PermissionService = prepareFdkPermissionsMock()

        val someBegrep = makeBegrep()//Same as mock pretends to save
        someBegrep.ansvarligVirksomhet = null

        val httpServletRequestMock: HttpServletRequest = mock()
        val rabbitPublisherMock: ConceptPublisher = prepareRabbitMock()
        val begreperApiImplK = BegreperApiImplK(begrepRepositoryMock, permissionServiceMock, rabbitPublisherMock)
        val retValue = begreperApiImplK.setBegrepById(httpServletRequestMock, "esf3", listOf(), true)
    }

    @Test
    fun test_deletion_both_checks_existence_and_actually_calls_delete() {
        val testBegrep = makeBegrep()
        val begrepRepositoryMock: BegrepRepository = prepareBegrepRepositoryMock(testBegrep)

        val permissionServiceMock: PermissionService = prepareFdkPermissionsMock()

        val httpServletRequestMock: HttpServletRequest = mock()
        val rabbitPublisherMock: ConceptPublisher = prepareRabbitMock()
        val begreperApiImplK = BegreperApiImplK(begrepRepositoryMock, permissionServiceMock, rabbitPublisherMock)
        val retValue = begreperApiImplK.deleteBegrepById(httpServletRequestMock, testBegrep.id)

        verify(begrepRepositoryMock).getBegrepById(testBegrep.id)
        verify(begrepRepositoryMock).removeBegrepById(testBegrep.id)
    }

    @Test
    fun test_liveness_and_readyness() {
        val begrepRepositoryMock: BegrepRepository = prepareBegrepRepositoryMock()
        val permissionServiceMock: PermissionService = prepareFdkPermissionsMock()

        val rabbitPublisherMock: ConceptPublisher = prepareRabbitMock()
        val begreperApiImplK = BegreperApiImplK(begrepRepositoryMock, permissionServiceMock, rabbitPublisherMock)
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
                definisjon = Definisjon().apply {
                    tekst = mapOf("nb" to "testbegrep")
                }
                ansvarligVirksomhet = createTestVirksomhet()
            }

    fun makeDataFilledBegrepA(): Begrep =
            Begrep().apply {
                id = "1c770979-34b0-439c-a7cb-adacb3619927"
                anbefaltTerm = Term().apply {
                    navn = mapOf("nb" to "fødestedA")
                }
                definisjon = Definisjon().apply {
                    tekst = mapOf("nb" to "testbegrepA")
                }
                ansvarligVirksomhet = createTestVirksomhet()
                status = Status.UTKAST
                kildebeskrivelse = Kildebeskrivelse()
                kildebeskrivelse.kilde = mutableListOf()
                val kilde = URITekst()
                kilde.tekst = "skatteetatenA"
                kilde.uri = "www.skatteetaten.noA"
                kildebeskrivelse.kilde.add(kilde)
                kildebeskrivelse.forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE
                merknad = mapOf("nb" to "testbegrepA")
                eksempel = mapOf("nb" to "bergenA")
                fagområde = mapOf("nb" to "fødeA")
                bruksområde = mapOf("nb" to listOf("medisinA"))
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
                anbefaltTerm = Term().apply {
                    navn = mapOf("nb" to "fødestedB")
                }
                definisjon = Definisjon().apply {
                    tekst = mapOf("nb" to "testbegrepB")
                }
                ansvarligVirksomhet = createTestVirksomhet()
                status = Status.UTKAST
                kildebeskrivelse = Kildebeskrivelse()
                val kilde = URITekst()
                kilde.tekst = "skatteetatenB"
                kilde.uri = "www.skatteetaten.noB"
                kildebeskrivelse.kilde.add(kilde)
                kildebeskrivelse.forholdTilKilde = Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT
                merknad = mapOf("nb" to "testbegrepB")
                eksempel = mapOf("nb" to "bergenB")
                fagområde = mapOf("nb" to "fødeB")
                bruksområde = mapOf("nb" to listOf("medisinB"))
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
                    path = "/definisjon/tekst/nb"
                    value = "testbegrepB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/anbefaltTerm/navn/nb"
                    value = "fødestedB"
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
                    path = "/merknad/nb"
                    value = "testbegrepB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/eksempel/nb"
                    value = "bergenB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/fagområde/nb"
                    value = "fødeB"
                },
                JsonPatchOperation().apply {
                    op = JsonPatchOperation.OpEnum.REPLACE
                    path = "/bruksområde/nb/0"
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

    private fun prepareBegrepRepositoryMock(begrep: Begrep = makeBegrep()): BegrepRepository {
        val begrepRepositoryMock: BegrepRepository = mock {}

        whenever(begrepRepositoryMock.getBegrepById(begrep.id)).thenReturn(begrep)
        whenever(begrepRepositoryMock.removeBegrepById(begrep.id)).thenReturn(1)

        return begrepRepositoryMock
    }

    private fun prepareFdkPermissionsMock(): PermissionService {
        val permissionServiceMock: PermissionService = mock {}

        whenever(permissionServiceMock.hasPublisherPermission(any(), any())).thenReturn(true)

        return permissionServiceMock
    }

    private fun prepareRabbitMock(virksomhet: Virksomhet = createTestVirksomhet()): ConceptPublisher {
        val conceptPublisher: ConceptPublisher = mock {}
        whenever(conceptPublisher.send(virksomhet.id)).then { doNothing() }
        return conceptPublisher
    }
}