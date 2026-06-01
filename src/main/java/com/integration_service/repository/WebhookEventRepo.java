package com.integration_service.repository;

import com.integration_service.entity.EventStatus;
import com.integration_service.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepo extends JpaRepository<WebhookEvent, String> {

    boolean existsByExternalEventId(String externalEventId);

    List<WebhookEvent> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetryCount);
}
