package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@Table(name = "pull_rerun_required")
@NoArgsConstructor
@AllArgsConstructor
public class PullRerunRequiredEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "created", nullable = false, insertable = false, updatable = false)
    private Instant created;

    @Column(name = "updated", nullable = false, insertable = false, updatable = false)
    private Instant updated;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oversikt_id", nullable = false)
    private AuditloggTaskEntity auditloggTask;

    @NotNull
    @Column(name = "pull_start_date", nullable = false)
    private LocalDate pullStartDate;

    @NotNull
    @Column(name = "pull_end_date", nullable = false)
    private LocalDate pullEndDate;

    @Size(max = 1000)
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @ColumnDefault("false")
    @Column(name = "resolved", nullable = false)
    private Boolean resolved;

    // Recommended equals and hashcode implementations for hibernate entities, use of lombok generated methods not recommended
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PullRerunRequiredEntity that = (PullRerunRequiredEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
