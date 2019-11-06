package no.brreg.conceptcatalogue.service;

import no.difi.skos_ap_no.concept.builder.ModelBuilder;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringReader;

import static org.junit.Assert.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class SkosApNoModelServiceTest {
    @Autowired
    private SkosApNoModelService skosApNoModelService;

    @Test
    public void expectEmptyModelToBeSerialisedAsTextTurtleCorrectly() {
        Model model = ModelBuilder.builder().build();
        String serialisedModel = skosApNoModelService.serializeAsTextTurtle(model);
        Model deserialisedModel = ModelBuilder.builder().build().read(new StringReader(serialisedModel), null, "TURTLE");

        assertTrue("Expect empty model to be serialised correctly in TURTLE format", deserialisedModel.isIsomorphicWith(model));
        assertTrue("Expect deserialised model to be empty", deserialisedModel.isEmpty());
    }
}
