package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.PullRerunJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

//TODO: expose secured controller making use of PullRerunService and DB2PacketService to manually re-run failed pulls
@Component
public class PullRerunService {

    private final PullRerunJPAAdapter adapter;

    @Autowired
    public PullRerunService(PullRerunJPAAdapter adapter) {
        this.adapter = adapter;
    }

    public void registerFailedPull(String dbname, TeknologiEnum teknologi, LocalDate pullStartDate, LocalDate pullEndDate, Throwable cause) {
        adapter.registerFailedPull(dbname, teknologi, pullStartDate, pullEndDate, describeFailure(cause));
    }

    public List<PullRerunRequiredDTO> findAllUnresolvedReruns() {
        return adapter.findAllUnresolvedReruns();
    }

    public void markRerunResolved(Long rerunId) {
        adapter.markRerunResolved(rerunId);
    }

    private String describeFailure(Throwable cause) {
        if (cause == null) {
            return null;
        }
        return cause.getClass().getName() + ": " + cause.getMessage();
    }
}
