package no.begrepskatalog.conceptregistration

import io.swagger.annotations.ApiParam
import no.begrepskatalog.conceptregistration.security.FdkPermissions
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.conceptregistration.utils.patchBegrep
import no.begrepskatalog.conceptregistration.validation.isValidBegrep
import no.begrepskatalog.generated.api.BegreperApi
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Endringslogelement
import no.begrepskatalog.generated.model.JsonPatchOperation
import no.begrepskatalog.generated.model.Status
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.json.JsonException
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val logger = LoggerFactory.getLogger(BegreperApiImplK::class.java)

@RestController
class BegreperApiImplK(val sqlStore: SqlStore, val fdkPermissions: FdkPermissions) : BegreperApi {

    @Value("\${application.baseURL}")
    lateinit var baseURL: String

    override fun getBegrep(httpServletRequest: HttpServletRequest?, @PathVariable orgnumber: String?, status: Status?): ResponseEntity<MutableList<Begrep>> {
        logger.info("Get begrep $orgnumber")
        if (orgnumber != null && fdkPermissions.hasPermission(orgnumber, "publisher", "admin")) {
            return ResponseEntity.ok(sqlStore.getBegrepByCompany(orgnumber, status))
        } else {
            //todo show list for all publishers  the user has access to
            return ResponseEntity.ok(mutableListOf())
        }
    }

    override fun createBegrep(httpServletRequest: HttpServletRequest, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep): ResponseEntity<Void> {

        begrep.id = null //We are the authority that provides ids
        begrep.updateLastChangedAndByWhom()

        if (!fdkPermissions.hasPermission(begrep?.ansvarligVirksomhet?.id, "publisher", "admin")) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

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

    override fun setBegrepById(httpServletRequest: HttpServletRequest?, id: String?, jsonPatchOperations: List<JsonPatchOperation>?, validate: Boolean?): ResponseEntity<Begrep> {
        if (id == null) {
            throw RuntimeException("Attempt to PATCH begrep with no id path variable given")
        }

        if (!fdkPermissions.hasPermission(id, "publisher", "admin")) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        if (jsonPatchOperations == null) {
            throw RuntimeException("Attempt to PATCH begrep with no changes provided. Id provided was $id")
        }

        if (!sqlStore.begrepExists(id)) {
            throw RuntimeException("Attempt to PUT begrep that does not already exist. Begrep id $id")
        }
        //Get the begrep, and just update
        var storedBegrep = sqlStore.getBegrepById(id)

        if (storedBegrep == null) {
            throw java.lang.RuntimeException("Attempt to PATCH begrep with id $id, that does not exist")
        }

        var patchedBegrep: Begrep?
        try {
            patchedBegrep = patchBegrep(storedBegrep, jsonPatchOperations)
        } catch (exception: Exception) {
            when (exception) {
                is JsonException, is IllegalArgumentException -> throw RuntimeException("Invalid patch operation. Begrep id $id")
                else -> throw exception
            }
        }

        patchedBegrep.updateLastChangedAndByWhom()

        if (patchedBegrep.status != Status.UTKAST && !isValidBegrep(patchedBegrep)) {
            logger.info("Begrep $patchedBegrep.id has not passed validation for non draft begrep and has not been saved ")
            return ResponseEntity(HttpStatus.CONFLICT)
        }

        sqlStore.saveBegrep(patchedBegrep)

        return ResponseEntity.ok(patchedBegrep)
    }

    override fun getBegrepById(httpServletRequest: HttpServletRequest, @ApiParam(value = "id", required = true) @PathVariable("id") id: String): ResponseEntity<Begrep> {

        if (!fdkPermissions.hasPermission(id, "publisher", "admin")) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

        val begrep = sqlStore.getBegrepById(id)

        return if (begrep != null) {
            ResponseEntity.ok(begrep)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
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

    override fun deleteBegrepById(httpServletRequest: HttpServletRequest, @ApiParam(value = "id", required = true) @PathVariable("id") id: String): ResponseEntity<Void> {

        if (!fdkPermissions.hasPermission(id, "publisher", "admin")) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }

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