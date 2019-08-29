package no.begrepskatalog.conceptregistration.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.begrepskatalog.generated.model.Begrep
import no.begrepskatalog.generated.model.JsonPatchOperation
import java.io.StringReader
import javax.json.Json
import javax.json.JsonException

@Throws(JsonException::class)
fun patchBegrep(begrep: Begrep, operations: List<JsonPatchOperation>): Begrep {
    if (operations.isNotEmpty()) {
        val mapper = ObjectMapper().registerModule(JavaTimeModule())
        val changes = Json.createReader(StringReader(mapper.writeValueAsString(operations))).readArray()

        val originalBegrepJsonObject = Json.createReader(StringReader(mapper.writeValueAsString(begrep))).readObject()
        val updatedBegrepJsonObject = Json.createPatch(changes).apply(originalBegrepJsonObject)

        return mapper.readValue(updatedBegrepJsonObject.toString(), Begrep::class.java)
    }
    return begrep
}
