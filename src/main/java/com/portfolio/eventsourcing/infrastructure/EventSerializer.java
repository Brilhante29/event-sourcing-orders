package com.portfolio.eventsourcing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.eventsourcing.domain.OrderEvent;
import org.springframework.stereotype.Component;

@Component
public class EventSerializer {
    private final ObjectMapper mapper;

    public EventSerializer() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String serialize(OrderEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event: " + event, e);
        }
    }

    public OrderEvent deserialize(String json) {
        try {
            return mapper.readValue(json, OrderEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + json, e);
        }
    }
}
