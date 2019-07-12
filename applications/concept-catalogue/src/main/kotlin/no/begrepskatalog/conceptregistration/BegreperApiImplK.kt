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
    lateinit var baseURL : String

    override fun getBegrep(httpServletRequest: HttpServletRequest?, @PathVariable orgnumber: String?, status: Status?): ResponseEntity<MutableList<Begrep>> {
        logger.info("Get begrep $orgnumber")
        var placeholderOrgnumber = "910244132"  //Ramsund og Rognand Revisjon

        val result: MutableList<Begrep> = sqlStore.getBegrepByCompany(orgNumber = placeholderOrgnumber)
        return ResponseEntity.ok(result)
    }

    override fun createBegrep(httpServletRequest: HttpServletRequest, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep): ResponseEntity<Void> {

        var storedBegrep = sqlStore.saveBegrep(begrep)
        if (storedBegrep != null) {
            logger.info("Stored begrep ${begrep.id}")
            val headers = HttpHeaders()
            val urlForAccessingThisBegrepsRegistration = baseURL + "/" + storedBegrep.ansvarligVirksomhet.id + "/" + storedBegrep.id
            headers.add(HttpHeaders.LOCATION, urlForAccessingThisBegrepsRegistration)
            return ResponseEntity(headers, HttpStatus.CREATED)
        } else {
            logger.info("Failed to store begrep. Reason should be in another log line.")
            return ResponseEntity(HttpStatus.CONFLICT)
        }
    }
}