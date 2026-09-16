package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface IdempotentMessageRepository extends JpaRepository<CamelMessageProcessedEntity, CamelMessageProcessedEntity.MessageId> {

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM CamelMessageProcessedEntity message
            WHERE message.id.processorName = :processorName
              AND message.createdAt < :cutoff
            """)
    int deleteMessagesOlderThan(@Param("processorName") String processorName, @Param("cutoff") Instant cutoff);
}
