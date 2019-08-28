package no.begrepskatalog.conceptregistration

import io.swagger.annotations.ApiParam
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.conceptregistration.validation.isValidBegrep
import no.begrepskatalog.generated.api.BegreperApi
import no.begrepskatalog.generated.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val logger = LoggerFactory.getLogger(BegreperApiImplK::class.java)

@RestController
@CrossOrigin(value = "*")
class BegreperApiImplK(val sqlStore: SqlStore) : BegreperApi {

    @Value("\${application.baseURL}")
    lateinit var baseURL: String

    override fun getBegrep(httpServletRequest: HttpServletRequest?, @PathVariable orgnumber: String?, status: Status?): ResponseEntity<MutableList<Begrep>> {
        logger.info("Get begrep $orgnumber")
        if (orgnumber != null) {
            return ResponseEntity.ok(sqlStore.getBegrepByCompany(orgnumber, status))
        } else {
            return ResponseEntity.ok(mutableListOf()) //TODO: When the publisher module is up and running, this will most likely be a fatal error
        }
    }

    override fun createBegrep(httpServletRequest: HttpServletRequest, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep): ResponseEntity<Void> {

        begrep.id = null //We are the authority that provides ids
        begrep.updateLastChangedAndByWhom()

        return sqlStore.saveBegrep(begrep)
                ?.let {
                    val headers = HttpHeaders()
                    val urlForAccessingThisBegrepsRegistration = baseURL + it.ansvarligVirksomhet.id + "/" + it.id
                    headers.add(HttpHeaders.LOCATION, urlForAccessingThisBegrepsRegistration)
                    headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION)
                    ResponseEntity<Void>(headers, HttpStatus.CREATED)
                }
                ?: let {
                    logger.info("Failed to store begrep. Reason should be in another log line.")
                    ResponseEntity<Void>(HttpStatus.CONFLICT)
                }
    }

    override fun setBegrepById(httpServletRequest: HttpServletRequest?, id: String?, begrep: Begrep?, validate: Boolean?): ResponseEntity<Begrep> {
        if (id == null) {
            throw RuntimeException("Attempt to PATCH begrep with no id path variable given")
        }
        if (begrep == null) {
            throw RuntimeException("Attempt to PATCH begrep with no begrep data given. Id provided was $id")
        }

        if (!sqlStore.begrepExists(id)) {
            throw RuntimeException("Attempt to PUT begrep that does not already exist. Begrep id ${begrep.id}")
        }
        //Get the begrep, and just update
        var storedBegrep = sqlStore.getBegrepById(id)

        if (storedBegrep == null) {
            throw java.lang.RuntimeException("Stored begrep with id $id was null. This should not happen")
        }

        val updatedBegrep = updateBegrep(begrep, storedBegrep)

        updatedBegrep.updateLastChangedAndByWhom()

        if (updatedBegrep.status == Status.UTKAST) {
            sqlStore.saveBegrep(updatedBegrep)
            return ResponseEntity.ok(updatedBegrep)
        } else {
            if (isValidBegrep(updatedBegrep)) {
                sqlStore.saveBegrep(updatedBegrep)
                logger.info("Begrep $updatedBegrep.id has passed validation for non draft begrep and has been saved ")
                return ResponseEntity.ok(updatedBegrep)
            }
        }
        return ResponseEntity(HttpStatus.CONFLICT)
    }

    private fun Begrep.updateLastChangedAndByWhom() {
        if (endringslogelement == null) {
            endringslogelement = Endringslogelement()
        }
        endringslogelement.apply {
            endringstidspunkt = OffsetDateTime.now()
            brukerId = "todo" //TODO: When auth is ready read username from auth context
        }
    }

    fun updateBegrep(source: Begrep, destination: Begrep): Begrep {
        if (source.status != null) {
            destination.status = source.status
        }
        if (source.anbefaltTerm != null) {
            destination.anbefaltTerm = source.anbefaltTerm
        }
        if (source.definisjon != null) {
            destination.definisjon = source.definisjon
        }
        if (source.kildebeskrivelse != null) {
            if (destination.kildebeskrivelse == null) {
                destination.kildebeskrivelse = Kildebeskrivelse()
            }
            if (source.kildebeskrivelse.forholdTilKilde!= null) {
                destination.kildebeskrivelse.forholdTilKilde= source.kildebeskrivelse.forholdTilKilde
            }
            if (source.kildebeskrivelse.kilde!= null ) {
                destination.kildebeskrivelse.kilde = source.kildebeskrivelse.kilde
            }
        } else {
            destination.kildebeskrivelse = null
        }
        if (source.merknad != null) {
            destination.merknad = source.merknad
        }
        if (source.eksempel != null) {
            destination.eksempel = source.eksempel
        }
        if (source.fagområde != null) {
            destination.fagområde = source.fagområde
        }
        if (source.bruksområde != null && source.bruksområde.isNotEmpty()) {
            destination.bruksområde = source.bruksområde
        }
        if (source.kontaktpunkt != null) {
            if (source.kontaktpunkt?.harEpost != null)
                destination.kontaktpunkt.harEpost = source.kontaktpunkt?.harEpost

            if (source.kontaktpunkt?.harTelefon != null)
                destination.kontaktpunkt.harTelefon = source.kontaktpunkt?.harTelefon
        }
        if (source.omfang != null) {
            if (source.omfang?.tekst != null)
                destination.omfang.tekst = source.omfang?.tekst
            if (source.omfang?.uri != null)
                destination.omfang.uri = source.omfang?.uri
        }
        if (source.gyldigFom != null) {
            destination.gyldigFom = source.gyldigFom
        }

        if (destination.endringslogelement == null) {
            destination.endringslogelement = Endringslogelement()
        }
        if (source.endringslogelement != null) {
            destination.endringslogelement.brukerId = source.endringslogelement.brukerId
            destination.endringslogelement.endringstidspunkt = source.endringslogelement.endringstidspunkt
        }
        if (source.tillattTerm != null && source.tillattTerm.isNotEmpty()) {
            destination.tillattTerm = source.tillattTerm
        }
        if (source.frarådetTerm != null && source.frarådetTerm.isNotEmpty()) {
            destination.frarådetTerm = source.frarådetTerm
        }

        return destination
    }

    override fun getBegrepById(httpServletRequest: HttpServletRequest, @ApiParam(value = "id", required = true) @PathVariable("id") id: String): ResponseEntity<Begrep> {
        val begrep = sqlStore.getBegrepById(id)

        return if (begrep != null) {
            ResponseEntity.ok(begrep)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    override fun deleteBegrepById(httpServletRequest: HttpServletRequest, @ApiParam(value = "id", required = true) @PathVariable("id") id: String): ResponseEntity<Void> {

        //Validate that begrep exists
        if (!sqlStore.begrepExists(id)) {
            logger.info("Request to delete non-existing begrep, id $id ignored")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }

        //Validate that begrep is NOT published
        val begrep = sqlStore.getBegrepById(id)

        if (begrep?.status == Status.PUBLISERT) {
            logger.warn("Attempt to delete PUBLISHED begrep $id ignored")
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        logger.info("Deleting begrep id $id organisation ${begrep?.ansvarligVirksomhet?.id}")
        sqlStore.deleteBegrepById(id)

        return ResponseEntity(HttpStatus.OK)
    }
}