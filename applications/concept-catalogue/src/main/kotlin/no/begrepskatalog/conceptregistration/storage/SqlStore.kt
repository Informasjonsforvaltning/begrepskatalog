package no.begrepskatalog.conceptregistration.storage

import no.begrepskatalog.generated.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.*
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val logger: Logger = LoggerFactory.getLogger(SqlStore::class.java)

@Component
class SqlStore(val connectionManager: ConnectionManager) {

    private val DB_STATUS_DRAFT = 1

    private val DB_STATUS_ACCEPTED = 2

    private val DB_STATUS_PUBLISHED = 3

    private val STRING_LIST_DELIMITER= "::"

    private val fetchURITextsByConceptURI = "select * from uritext u, conceptregistration_uritexts coupling where u.id = coupling.uritext and coupling.registration =  ? "

    private val fetchBegrepByCompanySQL = "select * from conceptregistrations c LEFT JOIN  conceptregistration.status s on c.status = s.id where ansvarlig_virksomhet = ? "

    private val fetchBegrepById = "select * from conceptregistrations c where id = ? "

    private val checkExistanceOfBegrep = "select * from conceptregistrations c where id = ?"

    private val fetchVirksomhetByOrg_Number = "select * from virksomhet where org_number = ?"

    private val fetchAllVirksomheter = "select * from virksomhet"

    private val saveVirksomhetSQL = "insert into virksomhet(org_number,uri,name,orgpath,preflabel) values (?,?,?,?,?) " +
            " ON CONFLICT (org_number) DO UPDATE " +
            " SET uri = excluded.uri;"

    private val createSource = "insert into URIText(uri,text) values(?,?)"

    private val addCupling = "insert into conceptregistration_URITexts(registration, URIText) values (?,?)"

    private val saveBegrepSQL = "insert into conceptregistrations(id,status,anbefalt_term,definisjon,forhold_til_kilde,merknad, " +
            " ansvarlig_virksomhet,eksempel,fagområde,bruksområde, omfang_tekst, omfang_uri,kontaktpunkt_harepost, kontaktpunkt_hartelefon,gyldig_fom,endret_av_brukernavn,sist_endret," +
            " tillatt_term, frarådet_term) " +
            " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

    private val updateBegrepSQL = "update conceptregistrations" +
            " set status=?,anbefalt_term=?,definisjon=?,forhold_til_kilde=?,merknad=?, " +
            " ansvarlig_virksomhet=?,eksempel=?,fagområde=?,bruksområde=?, omfang_tekst=?, omfang_uri=?,kontaktpunkt_harepost=?, kontaktpunkt_hartelefon=?,gyldig_fom=?, endret_av_brukernavn=?, sist_endret=?," +
            " tillatt_term=?, frarådet_term=?" +
            " where id=?"

    private val deleteBegrepSQL = "delete from conceptregistrations where id = ?"

    private val deleteKildeCouplingSQL = "delete from conceptregistration_URITexts where registration = ?"

    private val deleteURITextSQL = "delete from uritext as ut using conceptregistration_uritexts as coupling where (ut.id = coupling.uritext) and coupling.registration=?"

    private val getAllPublishedRegistrationsSQL = "select * from conceptregistrations c LEFT JOIN  conceptregistration.status s on c.status = s.id where c.status = " + DB_STATUS_PUBLISHED

    fun deleteBegrepById(id : String) {

        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(deleteBegrepSQL)
            stmt.setString(1, id)
            stmt.execute()
            var result = stmt.resultSet

            it.close()
        }
        deleteURITextForBegrep(id)
    }

    fun deleteURITextForBegrep(id : String) {

        logger.info("Trying to delete URIText and coupling for begrep id $id")
        connectionManager.getConnection().use {

            var couplingStmt = it.prepareStatement(deleteKildeCouplingSQL)
            couplingStmt.setString(1, id)
            couplingStmt.execute()
            var result = couplingStmt.resultSet

            var uritTextStmt = it.prepareStatement(deleteURITextSQL)
            uritTextStmt.setString(1,id)
            uritTextStmt.execute()

            logger.info("Deleted URIText and coupling for begrep id $id")
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
                var orgNumber = results.getString("ansvarlig_virksomhet")
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
        var create = false
        if (begrep.id == null) {
            val generatedId = java.util.UUID.randomUUID().toString()
            begrep.id = generatedId
            create = true
        }

        logger.info("Trying to store begrep ${begrep.id} to db. Create new begrep: $create ")

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

            val bruksområde: String? = begrep.bruksområde?.let { list -> list.joinToString(STRING_LIST_DELIMITER) }
            val tillattTerm: String? = begrep.tillattTerm?.let { list -> list.joinToString(STRING_LIST_DELIMITER) }
            val frarådetTerm: String? = begrep.frarådetTerm?.let { list -> list.joinToString(STRING_LIST_DELIMITER) }

            try {
                var begrepStmt : PreparedStatement? = null
                if (create) {
                    begrepStmt = it.prepareStatement(saveBegrepSQL)
                    begrepStmt?.setString(1, begrep.id)
                    begrepStmt?.setInt(2, mapStatusToInt(begrep.status))
                    begrepStmt?.setString(3, begrep.anbefaltTerm)
                    begrepStmt?.setString(4, begrep.definisjon)
                    begrepStmt?.setInt(5, mapForholdTilKildeToInt(begrep.kildebeskrivelse?.forholdTilKilde))
                    begrepStmt?.setString(6, begrep.merknad)
                    begrepStmt?.setString(7, virksomhet.id)
                    begrepStmt?.setString(8, begrep.eksempel)
                    begrepStmt?.setString(9, begrep.fagområde)
                    begrepStmt?.setString(10, bruksområde)
                    begrepStmt?.setString(11, begrep.omfang?.tekst)
                    begrepStmt?.setString(12, begrep.omfang?.uri)
                    begrepStmt?.setString(13, begrep.kontaktpunkt?.harEpost)
                    begrepStmt?.setString(14, begrep.kontaktpunkt?.harTelefon)

                    if (begrep.gyldigFom != null) {
                        begrepStmt?.setDate(15, Date.valueOf(begrep.gyldigFom))
                    } else {
                        begrepStmt?.setDate(15, null)
                    }

                    if (begrep.endringslogelement!=null) {

                        if (begrep.endringslogelement.brukerId!= null) {
                            begrepStmt?.setString(16,begrep.endringslogelement.brukerId)
                        } else {
                            begrepStmt?.setString(16,null)
                        }

                        if (begrep.endringslogelement.endringstidspunkt!= null ) {
                            begrepStmt?.setTimestamp(17, Timestamp.valueOf(begrep.endringslogelement.endringstidspunkt.toLocalDateTime()))
                        } else {
                            begrepStmt?.setTimestamp(17, null)
                        }
                    } else {
                        begrepStmt?.setString(16,null)
                        begrepStmt?.setTimestamp(17, null)
                    }

                    begrepStmt?.setString(18, tillattTerm)
                    begrepStmt?.setString(19, frarådetTerm)
                } else {
                    begrepStmt = it.prepareStatement(updateBegrepSQL)
                    begrepStmt?.setInt(1, mapStatusToInt(begrep.status))
                    begrepStmt?.setString(2, begrep.anbefaltTerm)
                    begrepStmt?.setString(3, begrep.definisjon)
                    begrepStmt?.setInt(4, mapForholdTilKildeToInt(begrep.kildebeskrivelse?.forholdTilKilde))
                    begrepStmt?.setString(5, begrep.merknad)
                    begrepStmt?.setString(6, virksomhet.id)
                    begrepStmt?.setString(7, begrep.eksempel)
                    begrepStmt?.setString(8, begrep.fagområde)
                    begrepStmt?.setString(9, bruksområde)
                    begrepStmt?.setString(10, begrep.omfang?.tekst)
                    begrepStmt?.setString(11, begrep.omfang?.uri)
                    begrepStmt?.setString(12, begrep.kontaktpunkt?.harEpost)
                    begrepStmt?.setString(13, begrep.kontaktpunkt?.harTelefon)
                    if (begrep.gyldigFom != null) {
                        begrepStmt?.setDate(14, Date.valueOf(begrep.gyldigFom))
                    } else {
                        begrepStmt?.setDate(14, null)
                    }

                    if (begrep.endringslogelement!=null) {

                        if (begrep.endringslogelement.brukerId!= null) {
                            begrepStmt?.setString(15,begrep.endringslogelement.brukerId)
                        } else {
                            begrepStmt?.setString(15,null)
                        }

                        if (begrep.endringslogelement.endringstidspunkt!= null ) {
                            begrepStmt?.setTimestamp(16, Timestamp.valueOf(begrep.endringslogelement.endringstidspunkt.toLocalDateTime()))
                        } else {
                            begrepStmt?.setTimestamp(16, null)
                        }
                    } else {
                        begrepStmt?.setString(15,null)
                        begrepStmt?.setTimestamp(16, null)
                    }

                    begrepStmt?.setString(17, tillattTerm)
                    begrepStmt?.setString(18, frarådetTerm)
                    begrepStmt?.setString(19, begrep.id)
                    //Delete whatever is in there, then write out
                    deleteURITextForBegrep(begrep.id)
                }

                begrepStmt?.execute()
                if (create) {
                    logger.info("Stored new begrep ${begrep.id}")
                } else {
                    logger.info("updated begrep ${begrep.id}")
                }
                //TODO: Remember transaction
                if ( begrep.kildebeskrivelse != null && begrep.kildebeskrivelse.kilde != null
                        && begrep.kildebeskrivelse.kilde.size > 0 ) {
                    for (kilde in begrep.kildebeskrivelse.kilde) {
                        storeSingleURIText(begrep.id, kilde)
                    }
                }

                return begrep
            } catch (t: Throwable) {
                t.printStackTrace()
                logger.error(t.toString())
            }
            it.close()
            return null
        }
    }

    private fun storeSingleURIText(id: String, kilde: URITekst) {
        connectionManager.getConnection().use {
            var stmtUriText = it.prepareStatement(createSource, Statement.RETURN_GENERATED_KEYS)
            stmtUriText.setString(1, kilde.uri)
            stmtUriText.setString(2, kilde.tekst)
            stmtUriText.execute()
            logger.info("Created URI text for begrep id $id, source was $kilde")

            var result = stmtUriText.resultSet

            //Get the new generated id from the db
            var generatedKeys = stmtUriText.getGeneratedKeys()
            generatedKeys.next()
            var idForUriText = generatedKeys.getInt(1)

            var stmtCoupling = it.prepareStatement(addCupling)
            stmtCoupling.setString(1,id)
            stmtCoupling.setInt(2,idForUriText)
            stmtCoupling.execute()

            it.close()
        }
    }

    fun mapForholdTilKildeToInt(forholdTilKilde: Kildebeskrivelse.ForholdTilKildeEnum?) : Int {
        if (forholdTilKilde == Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT) {
            return 1
        }
        if (forholdTilKilde == Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE) {
            return 2
        }
        if (forholdTilKilde == Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE) {
            return 3
        }
        return 1 //TODO: Actually die on this when the data is ok and frontend is ok
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

        val mappedBegrep = Begrep().apply {
            id = result.getString("id")
            status = mapStatus(result.getString("status"))
            anbefaltTerm = result.getString("anbefalt_term")
            definisjon = result.getString("definisjon")
            kildebeskrivelse = Kildebeskrivelse().apply {
                forholdTilKilde = mapForholdTilKilde(result.getString("forhold_til_kilde"))
                kilde = getSources(id)
            }
            merknad = result.getString("merknad")
            ansvarligVirksomhet = virksomhet
            eksempel = result.getString("eksempel")
            fagområde = result.getString("fagområde")
            bruksområde = result.getString("bruksområde")?.let { s -> s.split(STRING_LIST_DELIMITER).filter{ !it.isNullOrEmpty() } } ?: listOf()
            omfang = URITekst()
            omfang.tekst = result.getString("omfang_tekst")
            omfang.uri = result.getString("omfang_uri")
            kontaktpunkt = Kontaktpunkt().apply {
                harTelefon = result.getString("kontaktpunkt_hartelefon")
                harEpost = result.getString("kontaktpunkt_harepost")
            }
            if (result.getDate("gyldig_FOM") != null) {
                gyldigFom = result.getDate("gyldig_FOM").toLocalDate()
            }
            endringslogelement = Endringslogelement().apply {
                brukerId = result.getString("endret_av_brukernavn")
                if (result.getDate("sist_endret") != null && result.getTimestamp("sist_endret") != null) {
                    val endringsTidspunkt = result.getTimestamp("sist_endret")
                    endringstidspunkt = OffsetDateTime.of(endringsTidspunkt.toLocalDateTime(), ZoneOffset.ofHours(0))
                }
            }
            tillattTerm = result.getString("tillatt_term")?.let { s -> s.split(STRING_LIST_DELIMITER).filter{ !it.isNullOrEmpty() } } ?: listOf()
            frarådetTerm = result.getString("frarådet_term")?.let { s -> s.split(STRING_LIST_DELIMITER).filter{ !it.isNullOrEmpty() } } ?: listOf()
        }

        return mappedBegrep
    }

    private fun getSources(begrepId: String) : MutableList<URITekst>{
        val sources = mutableListOf<URITekst>()
        connectionManager.getConnection().use {
            var stmt = it.prepareStatement(fetchURITextsByConceptURI)
            stmt.setString(1, begrepId)
            var success = stmt.execute()

            var results = stmt.resultSet
            while (results.next()) {
                val uriText = URITekst()
                uriText.uri = results.getString("uri")
                uriText.tekst = results.getString("text")
                sources.add(uriText)
            }
        }
        return sources
    }

    fun mapVirksomhet(result: ResultSet): Virksomhet {

        val virksomhet = Virksomhet().apply {
            id = result.getString("org_number")
            uri = result.getString("uri")
            navn = result.getString("name")
            orgPath = result.getString("orgpath")
            prefLabel = result.getString("preflabel")
        }

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

    fun mapForholdTilKilde(forholdFromDb: String? ) : Kildebeskrivelse.ForholdTilKildeEnum {
        if (forholdFromDb == 1.toString()) {
            return Kildebeskrivelse.ForholdTilKildeEnum.EGENDEFINERT
        }
        if (forholdFromDb == 2.toString()) {
            return Kildebeskrivelse.ForholdTilKildeEnum.BASERTPAAKILDE
        }
        if (forholdFromDb == 3.toString()) {
            return Kildebeskrivelse.ForholdTilKildeEnum.SITATFRAKILDE
        }
        throw RuntimeException("While mapping ForholdTilKilde, encountered $forholdFromDb , that is not one of (1(EGENDEFINERT),2(BASERTPAAKILDE, 3(SITATFRAKILDE) )))")
    }
}