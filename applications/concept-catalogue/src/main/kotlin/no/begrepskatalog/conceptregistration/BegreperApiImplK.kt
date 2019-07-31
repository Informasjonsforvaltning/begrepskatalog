package no.begrepskatalog.conceptregistration

import io.swagger.annotations.ApiParam
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.api.BegreperApi
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
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
        var placeholderOrgnumber = "910244132"  //Ramsund og Rognand Revisjon

        val result: MutableList<Begrep> = sqlStore.getBegrepByCompany(orgNumber = placeholderOrgnumber)
        return ResponseEntity.ok(result)
    }

    override fun createBegrep(httpServletRequest: HttpServletRequest, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep): ResponseEntity<Void> {

        return sqlStore.saveBegrep(begrep)
                ?.let {
                    logger.info("Stored begrep ${it.id}")
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

        if (updatedBegrep.status == Status.UTKAST) {
            sqlStore.saveBegrep(updatedBegrep)
            return ResponseEntity(HttpStatus.OK)
        } else {
            if (updatedBegrep.anbefaltTerm != null && updatedBegrep.definisjon != null) {
                sqlStore.saveBegrep(updatedBegrep)
                logger.info("Begrep $updatedBegrep.id has passed validation for non draft begrep and has been saved ")
                return ResponseEntity.ok(begrep)
            }
        }
        return ResponseEntity(HttpStatus.CONFLICT)
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
        if (source.kilde != null) {
            destination.kilde = source.kilde
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
        if (source.bruksområde != null) {
            destination.bruksområde = source.bruksområde
        }
        if (source.verdiområde != null) {
            destination.verdiområde = source.verdiområde
        }
        if (source.kontaktpunkt != null) {
            destination.kontaktpunkt = source.kontaktpunkt
        }
        if (source.gyldigFom != null) {
            destination.gyldigFom = source.gyldigFom
        }
        if (source.forholdTilKilde != null) {
            destination.forholdTilKilde = source.forholdTilKilde
        }
        return destination
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