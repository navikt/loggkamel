package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

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
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@Table(name = "oversikt")
@NoArgsConstructor
@AllArgsConstructor
public class AuditloggTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ColumnDefault("NOW()")
    @Column(name = "created", nullable = false, insertable = false, updatable = false)
    private Instant created;

    @ColumnDefault("NOW()")
    @Column(name = "updated", nullable = false, insertable = false, updatable = false)
    private Instant updated;

    @Size(max = 100)
    @Column(name = "naisteam", length = 100)
    private String naisteam;

    @NotNull
    @Column(name = "teknologi", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TeknologiEnum teknologi;

    @Size(max = 100)
    @NotNull
    @Column(name = "dbname", nullable = false, length = 100)
    private String dbname;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "okonomi", nullable = false)
    private Boolean okonomi = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "arkivforskrift", nullable = false)
    private Boolean arkivforskrift = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "endringer_uten_krav", nullable = false)
    private Boolean endringerUtenKrav = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "logging_leseoperasjoner", nullable = false)
    private Boolean loggingLeseoperasjoner = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "fiksa", nullable = false)
    private Boolean fiksa = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "funnet_logger", nullable = false)
    private Boolean funnetLogger = false;

    @NotNull
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "discard_logs", nullable = false)
    private Boolean discardLogs = false;

    @Size(max = 22)
    @NotNull
    @Builder.Default
    @ColumnDefault("'backfill-completed'")
    @Column(name = "backfill", nullable = false, length = 22)
    private String backfill = "backfill-completed";

    // Recommended equals and hashcode implementations for hibernate entities, use of lombok generated methods not recommended
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AuditloggTaskEntity that = (AuditloggTaskEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}