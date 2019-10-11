package no.brreg.conceptcatalogue

import no.brreg.conceptcatalogue.validation.isValidBegrep
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*

class BegrepValidationTest {
    private fun createBegrep(): Begrep {
        return Begrep().apply {
            anbefaltTerm = "eplesaft"
            id = UUID.randomUUID().toString()
            ansvarligVirksomhet = createTestVirksomhet()
            bruksområde = listOf("Særavgift/Særavgift - Avgift på alkoholfrie drikkevarer")
            definisjon = "saft uten tilsatt sukker som er basert på epler"
            eksempel = "DummyEksempel"
            status = Status.GODKJENT
            gyldigFom = LocalDate.now()
        }
    }

    private fun createTestVirksomhet() : Virksomhet {
        return  Virksomhet().apply {
            id = "910244132"
            navn = "Ramsund og Rognand revisjon"
            orgPath = "/helt/feil/dummy/path"
            prefLabel =  "preflabel"
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

        begrep.anbefaltTerm = ""
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = "   "
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = "anbefaltTerm"
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to null)
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to "")
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to "  ")
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to "anbefaltTerm")
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to "anbefaltTerm", "en" to "recommendedTerm")
        Assert.assertTrue(isValidBegrep(begrep))

        begrep.anbefaltTerm = mapOf("nb" to "anbefaltTerm", "en" to null)
        Assert.assertTrue(isValidBegrep(begrep))
    }

    @Test
    fun testBegrepDefinisjonValidationMustBeSetAndNotEmpty() {
        val begrep = createBegrep()

        begrep.definisjon = null
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = ""
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = "   "
        Assert.assertFalse(isValidBegrep(begrep))

        begrep.definisjon = "definisjon"
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