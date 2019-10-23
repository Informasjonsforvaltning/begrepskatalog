package no.brreg.conceptcatalogue.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConceptPublisher {

    private final AmqpTemplate rabbitTemplate;

    public ConceptPublisher(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String publisherId) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("publisherId", publisherId);
        rabbitTemplate.convertAndSend(payload);
    }
}
