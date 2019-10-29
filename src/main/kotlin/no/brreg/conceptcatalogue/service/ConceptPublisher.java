package no.brreg.conceptcatalogue.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConceptPublisher {

    private final AmqpTemplate rabbitTemplate;

    private final static Logger logger = LoggerFactory.getLogger(ConceptPublisher.class);

    public ConceptPublisher(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String publisherId) {
        try {
            ObjectNode payload = JsonNodeFactory.instance.objectNode();
            payload.put("publisherId", publisherId);
            rabbitTemplate.convertAndSend(payload);
            logger.info("Successfully sent harvest message for publisher {}", publisherId);
        } catch (AmqpException e) {
            logger.error("Failed to send harvest message for publisher {}", publisherId, e);
        }
    }
}
