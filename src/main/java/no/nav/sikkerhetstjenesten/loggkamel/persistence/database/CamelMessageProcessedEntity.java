package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "camel_messageprocessed")
@NoArgsConstructor
public class CamelMessageProcessedEntity {

    @EmbeddedId
    private MessageId id;

    @Column(name = "createdat")
    private Instant createdAt;

    @Getter
    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageId implements Serializable {

        @Column(name = "processorname", nullable = false, length = 255)
        private String processorName;

        @Column(name = "messageid", nullable = false, length = 1000)
        private String messageId;
    }
}
