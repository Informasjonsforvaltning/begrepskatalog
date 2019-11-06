package no.brreg.conceptcatalogue.service

import org.apache.jena.rdf.model.Model
import org.springframework.stereotype.Service

import java.io.StringWriter

@Service
class SkosApNoModelService {
    fun serializeAsTextTurtle(model: Model): String {
        val stringWriter = StringWriter()
        model.write(stringWriter, "TURTLE")
        return stringWriter.buffer.toString()
    }
}
