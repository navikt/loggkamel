package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktService {
    private final OversiktJPAAdapter adapter;

    @Autowired
    public OversiktService(OversiktJPAAdapter adapter) {
        this.adapter = adapter;
    }

    public AuditloggTaskDTO createAuditloggTask(AuditloggTaskRequestDTO request) {
        return adapter.createAuditloggTask(request);
    }

    public AuditloggTaskDTO updateAuditloggTask(AuditloggTaskRequestDTO request) {
        return adapter.updateAuditloggTask(request);
    }

    public AuditloggTaskDTO getAuditloggTaskByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public void registerLogsReceivedForAuditloggTask(String dbname, TeknologiEnum teknologi) {
        adapter.registerLogsReceivedForAuditloggTask(dbname, teknologi);
    }

    public List<AuditloggTaskDTO> getAuditloggTaskByNaisteam(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam);
    }

    public boolean naisteamHasActiveAuditloggTasks(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam).stream()
                .anyMatch(task -> task.getFiksa() && (task.getLoggingLeseoperasjoner() || task.getLoggingEndringer()));
    }

    public List<String> findAllNaisteamWithActiveAuditloggTasks() {
        return adapter.findAllDistinctNaisteam().stream()
                .filter(this::naisteamHasActiveAuditloggTasks)
                .toList();
    }
}
