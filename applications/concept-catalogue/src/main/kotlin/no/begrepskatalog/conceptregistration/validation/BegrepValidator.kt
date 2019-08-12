package no.begrepskatalog.conceptregistration.validation

import no.begrepskatalog.generated.model.*
import org.hibernate.validator.internal.util.ModUtil

fun isValidBegrep(begrep: Begrep): Boolean = when {
    begrep.status == null -> false
    begrep.status == Status.UTKAST -> false
    begrep.anbefaltTerm.isNullOrBlank() -> false
    begrep.definisjon.isNullOrBlank() -> false
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