package no.brreg.conceptcatalogue

import no.begrepskatalog.generated.model.*
import no.brreg.conceptcatalogue.validation.isValidBegrep
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*

class BegrepValidationTest {
    private fun createBegrep(): Begrep {
        return Begrep().apply {
            anbefaltTerm = Term().apply {
                navn = mapOf("no" to "eplesaft")
            }
            id = UUID.randomUUID().toString()
            ansvarligVirksomhet = createTestVirksomhet()
            bruksområde = listOf("Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer")
            definisjon = Definisjon().apply {
                tekst = mapOf("no" to "saft uten tilsatt sukker som er basert på epler")
            }
            eksempel = "DummyEksempel"
            status = Status.GODKJENT
            gyldigFom = LocalDate.now()
        }
    }

    private fun createTestVirksomhet(): Virksomhet {
        return Virksomhet().apply {
            id = "910244132"
            navn = "Ramsund og Rognand revisjon"
            orgPath = "/helt/feil/dummy/path"
            prefLabel = "preflabel"
            uri = "ramsumdURI"
        }
    }

    @Test
    fun testBegrepStatusMustNotBeUtkastDuringValidation() {
        val begrep = createBegrep()

        begrep.status = Status.PUBLISERT
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.status = Status.GODKJENT
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.status = Status.UTKAST
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.status = null
        Assert.assertFalse(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepAnbefaltTermMustBeSetAndNotEmpty() {
        val begrep = createBegrep()

        begrep.anbefaltTerm = null
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term()
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("" to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("   " to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("" to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("   " to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "   ")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst", null to null)
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst", "kode2" to null)
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst", "kode2" to "")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst", "kode2" to "   ")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = Term().apply {
            navn = mapOf("kode" to "tekst", "kode2" to "tekst2")
        }
        Assert.assertTrue(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepDefinisjonValidationMustBeSetAndNotEmpty() {
        val begrep = createBegrep()

        begrep.definisjon = null
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon()
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("" to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("   " to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode" to null)
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("" to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("   " to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode" to "")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode" to "   ")
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode" to "tekst")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode1" to "tekst1", null to null)
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode1" to "tekst1", "kode2" to null)
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode1" to "tekst1", "kode2" to "")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode1" to "tekst1", "kode2" to "   ")
        }
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.definisjon = Definisjon().apply {
            tekst = mapOf("kode1" to "tekst1", "kode2" to "tekst2")
        }
        Assert.assertTrue(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepAnsvarligVirksomhetMustHaveAValidOrganisationNumber() {
        val begrep = createBegrep()

        begrep.ansvarligVirksomhet = null
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = null
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = ""
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "123"
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "123456789"
        }
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet()
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.ansvarligVirksomhet = createTestVirksomhet().apply {
            id = "943574537"
        }
        Assert.assertTrue(isValidBegrep(begrep))
    }
}