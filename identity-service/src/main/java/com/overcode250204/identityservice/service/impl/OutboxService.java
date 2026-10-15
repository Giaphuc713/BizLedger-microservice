package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.service.IOutboxService;
import com.overcode250204.identityservice.outbox.OutboxHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService implements IOutboxService {

    private final OutboxHelper outboxHelper;

    @Value("${spring.application.name}")
    private String applicationName;


    @Override
    public void saveEvent(
            String topic,
            String eventType,
            UUID tenantId,
            String traceId,
            Map<String, Object> data) {
        outboxHelper.saveEvent(topic, eventType, applicationName, tenantId, traceId, data);
    }

}
