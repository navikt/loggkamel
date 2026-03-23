package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OversiktEntity {

    @Id
    int id;

    @Column
    String naisteam;

    @Column
    TeknologiEnum teknologi;

    @Column(unique = true)
    String dbname;

    @Column
    boolean okonomi = false;

    @Column
    boolean arkiv = false;

    @Column
    boolean personvern = false;

    @Column
    boolean fiksa = false;

    @Column
    @CreatedDate
    Instant created;

    @Column
    @LastModifiedDate
    Instant updated;
}

