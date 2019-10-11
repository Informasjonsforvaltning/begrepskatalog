package no.brreg.conceptcatalogue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConceptRegistrationApplication

fun main(args: Array<String>) {
	runApplication<ConceptRegistrationApplication>(*args)
}
