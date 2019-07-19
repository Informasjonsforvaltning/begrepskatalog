package no.begrepskatalog.conceptregistration.storage

import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.Status
import no.begrepskatalog.generated.model.Virksomhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.ResultSet

private val logger: Logger = LoggerFactory.getLogger(SqlStore::class.java)

@Component
class SqlStore(val connectionManager: ConnectionManager) {

    private val DB_STATUS_DRAFT = 1

    private val DB_STATUS_ACCEPTED = 2

    private val DB_STATUS_PUBLISHED = 3

    private val fetchBegrepByCompanySQL = "select * from conceptregistrations c LEFT JOIN  conceptregistration.status s on c.status = s.id where ansvarlig_virksomhet = ? "

    private val fetchBegrepById = "select * from conceptregistrations c where id = ? "

    private val checkExistanceOfBegrep = "select * from conceptregistrations c where id = ?"

    private val fetchVirksomhetByOrg_Number = "select * from virksomhet where org_number = ?"

    private val fetchAllVirksomheter = "select * from virksomhet"

    private val saveVirksomhetSQL = "insert into virksomhet(org_number,uri,name,orgpath,preflabel) values (?,?,?,?,?) " +
            " ON CONFLICT (org_number) DO UPDATE " +
            " SET uri = excluded.uri;"

    private val saveBegrepSQL = "insert into conceptregistrations(id,status,anbefalt_term,definisjon,kilde,merknad, " +
            " ansvarlig_virksomhet,eksempel,fagområde,bruksområde, verdiområde,kontaktpunkt,gyldig_fom,forhold_til_kilde) " +
            " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

    private val deleteBegrepSQL = "delete from conceptregistrations where id = ?"

    private val getAllPublishedRegistrationsSQL = "select * from conceptregistrations c LEFT JOIN  conceptregistration.status s on c.status = s.id where c.status = " + DB_STATUS_PUBLISHED

    fun deleteBegrepById(id : String) {
        //delete the thing
        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(deleteBegrepSQL)
            stmt.setString(1, id)
            stmt.execute()
            var result = stmt.resultSet

            it.close()
        }
    }

    fun getAllPublishedBegrep() : List<Begrep>{
        val begrepList = mutableListOf<Begrep>()

        val allVirksomheter = getAllVirksomheter()

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(getAllPublishedRegistrationsSQL)
            stmt.execute()
            var result = stmt.resultSet
            while (result.next()) {
                val virksomhetId = result.getString("ansvarlig_virksomhet")
                val virksomhet = allVirksomheter.get(virksomhetId)
                if (virksomhet!= null) {
                    begrepList.add(mapToBegrep(result,virksomhet))
                } else {
                    throw java.lang.RuntimeException("Database had begrep which had no corresponding ansvarlig virksomhet")
                }
            }
            it.close()
            return begrepList
        }
    }

    fun getAllVirksomheter() : Map<String, Virksomhet>{
        val virksomheter = mutableMapOf<String, Virksomhet>()

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(fetchAllVirksomheter)
            stmt.execute()
            var result = stmt.resultSet
            while (result.next()) {
                virksomheter.put(result.getString("org_number"), mapVirksomhet(result))
            }
            it.close()
            return virksomheter
        }
    }

    fun getBegrepById(id :String) : Begrep? {
        var begrep : Begrep

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(fetchBegrepById)
            stmt.setString(1, id)
            var success = stmt.execute()

            var results = stmt.resultSet
            if (results.next()) {
                var orgNumber = results.getString("org_number")
                var thisVirksomhet = getVirksomhet(orgNumber)

                if (thisVirksomhet == null) {
                    logger.info("In getBegrepById : failed to find virksomhet $orgNumber, that begrep $id claims to belong to")
                    return null
                }
                begrep = mapToBegrep(results, thisVirksomhet )
                it.close()
                return begrep
            }
        }
        return null
    }

    fun getBegrepByCompany(orgNumber: String): MutableList<Begrep> {
        logger.info(connectionManager.toString())
        logger.info("Retrieving concepts for organization $orgNumber.")
        var begrepList = mutableListOf<Begrep>()

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(fetchBegrepByCompanySQL)
            stmt.setString(1, orgNumber)
            var success = stmt.execute()


            val thisVirksomhet = getVirksomhet(orgNumber)
            if (thisVirksomhet == null) {
                logger.info("In GetBegrepByCompany: failed to find virksomhet $orgNumber, can thus not find Begrep")
                return begrepList
            }
            logger.info("Retrieving Virksomhet for organization number $orgNumber. Got $thisVirksomhet")

            var results = stmt.resultSet
            logger.info("Results object : ${results}")
            while (results.next()) {
                begrepList.add(mapToBegrep(results, thisVirksomhet))
            }
            it.close()
            return begrepList
        }
    }

    fun getVirksomhet(orgNumber: String): Virksomhet? {
        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(fetchVirksomhetByOrg_Number)
            stmt.setString(1, orgNumber)
            var success = stmt.execute()
            var result = stmt.resultSet
            while (result.next()) {
                return mapVirksomhet(result)
            }
            it.close()
            return null
        }
    }
    fun begrepExists(begrep: Begrep): Boolean {
        if (begrep == null || begrep.id == null) {
            return false
        }
        return begrepExists(begrep.id)
    }

    fun begrepExists(id : String) : Boolean {
        if (id == null || id.isNullOrEmpty()) {
            return false
        }

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(checkExistanceOfBegrep)
            stmt.setString(1, id)
            stmt.execute()
            var result = stmt.resultSet
            if (result.next()) {//There was a result
                it.close()
                return true
            }
            it.close()
            return false
        }
    }

    //Precondition: Virksomhet is never null
    fun saveBegrep(begrep: Begrep): Begrep? {

        val generatedId = java.util.UUID.randomUUID().toString()
        begrep.id = generatedId

        logger.info("Trying to store begrep ${generatedId} to db")

        val virksomhet = begrep.ansvarligVirksomhet

        connectionManager.getConnection().use {

            //save virksomhet if it is not already known
            val storedVirksomhet = getVirksomhet(virksomhet.id)
            if (storedVirksomhet == null) {
                val virksomhetStmt = it.prepareStatement(saveVirksomhetSQL)
                virksomhetStmt.setString(1, virksomhet.id) //org number
                virksomhetStmt.setString(2, virksomhet.uri)
                virksomhetStmt.setString(3, virksomhet.navn)
                virksomhetStmt.setString(4, virksomhet.orgPath)
                virksomhetStmt.setString(5, virksomhet.prefLabel)

                virksomhetStmt.execute()
                logger.info("Virksomhet ${virksomhet.id} stored")
            } else {
                logger.info("Virksomhet ${storedVirksomhet.id} already stored")
            }

            try {
                val begrepStmt = it.prepareStatement(saveBegrepSQL)
                begrepStmt.setString(1, begrep.id)
                begrepStmt.setInt(2, mapStatusToInt(begrep.status))
                begrepStmt.setString(3, begrep.anbefaltTerm)
                begrepStmt.setString(4, begrep.definisjon)
                begrepStmt.setString(5, begrep.kilde)
                begrepStmt.setString(6, begrep.merknad)
                begrepStmt.setString(7, virksomhet.id)
                begrepStmt.setString(8, begrep.eksempel)
                begrepStmt.setString(9, begrep.fagområde)
                begrepStmt.setString(10, begrep.bruksområde)
                begrepStmt.setString(11, begrep.verdiområde)
                begrepStmt.setString(12, begrep.kontaktpunkt)
                if (begrep.gyldigFom != null) {
                    begrepStmt.setDate(13, Date.valueOf(begrep.gyldigFom))
                } else {
                    begrepStmt.setDate(13, null)
                }
                begrepStmt.setString(14, begrep.forholdTilKilde)

                begrepStmt.execute()
                logger.info("Stored begrep ${begrep.id}")
                return begrep
            } catch (t: Throwable) {
                t.printStackTrace()
                logger.error(t.toString())
            }
            it.close()
            return null
        }
    }

    fun mapStatusToInt(status: Status): Int {
        if (status == Status.UTKAST) {
            return 1
        }
        if (status == Status.GODKJENT) {
            return 2
        }
        if (status == Status.PUBLISERT) {
            return 3
        }
        throw RuntimeException("Error converting status to db ids, Got ${status} Did not get one of (UTKAST,GODKJENT,PUBLISERT)")
    }

    fun mapToBegrep(result: ResultSet, virksomhet: Virksomhet): Begrep {

        val mappedBegrep = Begrep()
        mappedBegrep.id = result.getString("id")
        mappedBegrep.status = mapStatus(result.getString("status"))
        mappedBegrep.anbefaltTerm = result.getString("anbefalt_term")
        mappedBegrep.definisjon = result.getString("definisjon")
        mappedBegrep.kilde = result.getString("kilde")
        mappedBegrep.merknad = result.getString("merknad")
        mappedBegrep.ansvarligVirksomhet = virksomhet
        mappedBegrep.eksempel = result.getString("eksempel")
        mappedBegrep.fagområde = result.getString("fagområde")
        mappedBegrep.bruksområde = result.getString("bruksområde")
        mappedBegrep.verdiområde = result.getString("verdiområde")
        mappedBegrep.kontaktpunkt = result.getString("kontaktpunkt")
        if (result.getDate("gyldig_FOM") != null) {
            mappedBegrep.gyldigFom = result.getDate("gyldig_FOM").toLocalDate()
        }
        mappedBegrep.forholdTilKilde = result.getString("forhold_til_kilde")

        return mappedBegrep
    }

    fun mapVirksomhet(result: ResultSet): Virksomhet {

        val virksomhet = Virksomhet()
        virksomhet.id = result.getString("org_number")
        virksomhet.uri = result.getString("uri")
        virksomhet.navn = result.getString("name")
        virksomhet.orgPath = result.getString("orgpath")
        virksomhet.prefLabel = result.getString("preflabel")
        return virksomhet
    }

    fun mapStatus(statusFromDb: String): Status {

        if (statusFromDb == 1.toString()) {
            return Status.UTKAST
        }
        if (statusFromDb == 2.toString()) {
            return Status.GODKJENT
        }
        if (statusFromDb == 3.toString()) {
            return Status.PUBLISERT
        }
        throw RuntimeException("While mapping statuses, encountered status $statusFromDb , that is not one of (1(UTKAST),2(GODKJENT),3(PUBLISERT))")
    }
}