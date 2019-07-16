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
import org.springframework.web.bind.annotation.*
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
                    val urlForAccessingThisBegrepsRegistration = baseURL + "/" + it.ansvarligVirksomhet.id + "/" + it.id
                    headers.add(HttpHeaders.LOCATION, urlForAccessingThisBegrepsRegistration)
                    ResponseEntity<Void>(headers, HttpStatus.CREATED)
                }
                ?: let {
                    logger.info("Failed to store begrep. Reason should be in another log line.")
                    ResponseEntity<Void>(HttpStatus.CONFLICT)
                }
    }

    override fun setBegrepById(httpServletRequest: HttpServletRequest, @ApiParam(value = "id", required = true) id: String, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep, @ApiParam(value = "Om begrepet skal valideres") @Valid @RequestParam(value = "validate", required = false) validate: Boolean): ResponseEntity<Begrep> {
        //We must have an ID that already exists, AND a virksomhet
        if (!sqlStore.begrepExists(begrep)) {
            throw RuntimeException("Attempt to PUT begrep that does not already exist. Begrep id ${begrep.id}")
        }

        if (begrep.ansvarligVirksomhet == null) {
            throw RuntimeException("Attempt to PUT begrep with no accompanying Ansvarlig Virksomhet")
        }

        if (begrep.status == Status.UTKAST) {
            sqlStore.saveBegrep(begrep)
            return ResponseEntity(HttpStatus.OK)
        } else {
            if (begrep.anbefaltTerm != null && begrep.definisjon != null) {
                sqlStore.saveBegrep(begrep)
                logger.info("Begrep $begrep.id has passed validation for non draft begrep and has been saved ")
                return ResponseEntity(HttpStatus.OK)
            }
        }

        return ResponseEntity(HttpStatus.CONFLICT)
    }
}