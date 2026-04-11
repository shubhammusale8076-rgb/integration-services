package com.integration_service.repository;

import com.integration_service.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageLogRepo extends JpaRepository<MessageLog, String> {

    Optional<MessageLog> findByMessageId(String messageId);
}
