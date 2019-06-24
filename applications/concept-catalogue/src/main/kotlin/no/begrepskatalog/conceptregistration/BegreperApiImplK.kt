package no.begrepskatalog.conceptregistration

import io.swagger.annotations.ApiParam
import no.begrepskatalog.conceptregistration.storage.SqlStore
import no.begrepskatalog.generated.api.BegreperApi
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@RestController
class BegreperApiImplK : BegreperApi {

    private val logger = LoggerFactory.getLogger(BegreperApiImplK::class.java)

    @Autowired
    lateinit var sqlStore: SqlStore

    override fun getBegrep(httpServletRequest: HttpServletRequest?, @PathVariable orgnumber: String?, status: Status?): ResponseEntity<MutableList<Begrep>> {
        logger.info("Get begrep $orgnumber")
        var placeholderOrgnumber = "910244132"  //Ramsund og Rognand Revisjon

        val result: MutableList<Begrep> = sqlStore.getBegrepByCompany(orgNumber = placeholderOrgnumber)
        return ResponseEntity.ok(result)
    }

    override fun createBegrep(httpServletRequest: HttpServletRequest, @ApiParam(value = "", required = true) @Valid @RequestBody begrep: Begrep): ResponseEntity<Void> {
        logger.info("Create Begrep called with begrep id  ${begrep.id}")
        var successfulStore = sqlStore.saveBegrep(begrep)
        if (successfulStore) {
            logger.info("Stored begrep ${begrep.id}")
        } else {
            logger.info("Failed to store begrep. Reason should be in another log line.")
        }
        return ResponseEntity.of(Optional.empty())
    }
}