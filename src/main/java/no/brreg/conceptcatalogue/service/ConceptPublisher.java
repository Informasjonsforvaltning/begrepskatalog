package no.brreg.conceptcatalogue.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.brreg.conceptcatalogue.model.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConceptPublisher {

    private final AmqpTemplate rabbitTemplate;

    private final static Logger logger = LoggerFactory.getLogger(ConceptPublisher.class);

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    public ConceptPublisher(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String publisherId) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("publisherId", publisherId);

        try {
            rabbitTemplate.convertAndSend(payload);
            logger.info("Successfully sent harvest message for publisher {}", publisherId);
        } catch (AmqpException e) {
            logger.error("Failed to send harvest message for publisher {}", publisherId, e);
        }
    }

    public void sendNewDataSource(String publisherId, String harvestUrl) {
        DataSource dataSource = DataSource.builder()
            .publisherId(publisherId)
            .dataSourceType(DataSource.DataSourceTypeEnum.SKOS_AP_NO)
            .acceptHeaderValue("text/turtle")
            .description(String.format("Automatically generated data source for %s", publisherId))
            .url(harvestUrl)
            .build();

        try {
            rabbitTemplate.convertAndSend(exchange, "conceptPublisher.NewDataSource", dataSource);
            logger.info("Successfully sent new datasource message for publisher {}", publisherId);
        } catch (AmqpException e) {
            logger.error("Failed to send new datasource message for publisher {}", publisherId, e);
        }
    }
}
