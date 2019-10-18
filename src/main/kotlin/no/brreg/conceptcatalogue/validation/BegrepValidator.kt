package no.brreg.conceptcatalogue.validation

import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.hibernate.validator.internal.util.ModUtil

fun isValidBegrep(begrep: Begrep): Boolean = when {
    begrep.status == null -> false
    begrep.status == Status.UTKAST -> false
    begrep.anbefaltTerm == null -> false
    begrep.anbefaltTerm.navn === null -> false
    begrep.anbefaltTerm.navn.isEmpty() -> false
    !isValidListOfTranslations(begrep.anbefaltTerm.navn) -> false
    begrep.definisjon == null -> false
    begrep.definisjon.tekst === null -> false
    begrep.definisjon.tekst.isEmpty() -> false
    !isValidListOfTranslations(begrep.definisjon.tekst) -> false
    begrep.ansvarligVirksomhet == null -> false
    !begrep.ansvarligVirksomhet.isValid() -> false
    else -> true
}

private fun Virksomhet.isValid(): Boolean = when {
    id.isNullOrBlank() -> false
    id.length != 9 -> false
    !isValidOrganisationNumber(id) -> false
    else -> true
}

private fun isValidOrganisationNumber(organisationNumber: String): Boolean {
    val organisationNumberWithoutControlDigit: String = organisationNumber.dropLast(1)
    val organisationNumberDigits: List<Int> = organisationNumberWithoutControlDigit
            .split("")
            .filter { !it.isBlank() }
            .map { it.toInt() }
    val controlDigit: Int = ModUtil.calculateMod11Check(organisationNumberDigits, 7)
    return organisationNumber == "$organisationNumberWithoutControlDigit$controlDigit"
}

private fun isValidListOfTranslations(translations: Map<String, String>): Boolean = when {
    translations is Map<String, String> && !translations.values.stream().anyMatch { it is String && !it.isNullOrBlank() } -> false
    else -> true
}