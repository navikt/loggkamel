package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class PullRerunJPAAdapter {

    static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final PullRerunRequiredRepository pullRerunRequiredRepository;
    private final OversiktRepository oversiktRepository;
    private final PullRerunRequiredMapper mapper;

    @Autowired
    public PullRerunJPAAdapter(PullRerunRequiredRepository pullRerunRequiredRepository, OversiktRepository oversiktRepository, PullRerunRequiredMapper mapper) {
        this.pullRerunRequiredRepository = pullRerunRequiredRepository;
        this.oversiktRepository = oversiktRepository;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedPull(String dbname, TeknologiEnum teknologi, LocalDate pullStartDate, LocalDate pullEndDate, String failureReason) {
        AuditloggTaskEntity auditloggTask = oversiktRepository.findByDbnameAndTeknologi(dbname, teknologi);
        if (auditloggTask == null) {
            throw new IllegalStateException("Cannot register failed pull, no Arkiv task exists for dbname " + dbname + " and teknologi " + teknologi);
        }

        String truncatedFailureReason = truncateFailureReason(failureReason);

        PullRerunRequiredEntity toSave = pullRerunRequiredRepository
                .findUnresolvedForTaskAndRange(auditloggTask, pullStartDate, pullEndDate)
                .orElseGet(() -> PullRerunRequiredEntity.builder()
                        .auditloggTask(auditloggTask)
                        .pullStartDate(pullStartDate)
                        .pullEndDate(pullEndDate)
                        .resolved(false)
                        .build());

        toSave.setFailureReason(truncatedFailureReason);
        pullRerunRequiredRepository.save(toSave);
    }

    @Transactional(readOnly = true)
    public List<PullRerunRequiredDTO> findAllUnresolvedReruns() {
        return pullRerunRequiredRepository.findAllUnresolved().stream()
                .map(mapper::pullRerunRequiredEntityToDTO)
                .toList();
    }

    @Transactional
    public void markRerunResolved(Long rerunId) {
        PullRerunRequiredEntity toResolve = pullRerunRequiredRepository.findById(rerunId)
                .orElseThrow(() -> new IllegalArgumentException("No pull rerun entry exists with id " + rerunId));
        toResolve.setResolved(true);
        pullRerunRequiredRepository.save(toResolve);
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null) {
            return null;
        }
        return failureReason.length() <= MAX_FAILURE_REASON_LENGTH ? failureReason : failureReason.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
