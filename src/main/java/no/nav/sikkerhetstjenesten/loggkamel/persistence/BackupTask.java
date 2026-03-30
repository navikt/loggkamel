package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@Table(name = "oversikt")
@NoArgsConstructor
@AllArgsConstructor
public class BackupTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ColumnDefault("now()")
    @Column(name = "created")
    private Instant created;

    @ColumnDefault("now()")
    @Column(name = "updated")
    private Instant updated;

    @Size(max = 100)
    @NotNull
    @Column(name = "naisteam", nullable = false, length = 100)
    private String naisteam;

    @Column(name = "teknologi")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TeknologiEnum teknologi;

    @Size(max = 100)
    @NotNull
    @Column(name = "dbname", nullable = false, length = 100)
    private String dbname;

    @ColumnDefault("false")
    @Column(name = "okonomi")
    private Boolean okonomi;

    @ColumnDefault("false")
    @Column(name = "arkiv")
    private Boolean arkiv;

    @ColumnDefault("false")
    @Column(name = "personvern")
    private Boolean personvern;

    @ColumnDefault("false")
    @Column(name = "fiksa")
    private Boolean fiksa;

}