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
public class AuditLoggArkivEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "created", nullable = false, insertable = false, updatable = false)
    private Instant created;

    @Column(name = "updated", nullable = false, insertable = false, updatable = false)
    private Instant updated;

    @Size(max = 100)
    @NotNull
    @Column(name = "naisteam", nullable = false, length = 100)
    private String naisteam;

    @NotNull
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

    // Recommended equals and hashcode implementations for hibernate entities, use of lombok generated methods not recommended
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AuditLoggArkivEntity that = (AuditLoggArkivEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}