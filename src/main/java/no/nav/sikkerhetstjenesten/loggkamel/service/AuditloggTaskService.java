package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisTeamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditloggTaskService {
    private final OversiktJPAAdapter adapter;

    @Autowired
    public AuditloggTaskService(OversiktJPAAdapter adapter) {
        this.adapter = adapter;
    }

    public AuditloggTaskDTO createAuditloggTask(AuditloggTaskRequestDTO auditloggTaskToCreate, List<String> naisteamsForUser) {
        requireMembership(auditloggTaskToCreate.getNaisteam(), naisteamsForUser);
        return adapter.createAuditloggTask(auditloggTaskToCreate);
    }

    public AuditloggTaskDTO updateAuditloggTask(AuditloggTaskRequestDTO auditloggTaskToCreate, List<String> naisteamsForUser) {
        requireMembership(auditloggTaskToCreate.getNaisteam(), naisteamsForUser);

        AuditloggTaskDTO existingTask = adapter.findByDbnameAndTeknologi(auditloggTaskToCreate.getDbname(), auditloggTaskToCreate.getTeknologi());
        if (existingTask != null) {
            requireMembership(existingTask.getNaisteam(), naisteamsForUser);
        }

        return adapter.updateAuditloggTask(auditloggTaskToCreate);
    }

    private static void requireMembership(String naisteam, List<String> naisteamsForUser) {
        if (naisteamsForUser == null || !naisteamsForUser.contains(naisteam)) {
            throw new ForbiddenOperationException("Brukeren er ikke medlem av naisteam " + naisteam);
        }
    }

    public AuditloggTaskDTO getAuditloggTaskByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public void registerLogsReceivedForAuditloggTask(String dbname, TeknologiEnum teknologi) {
        adapter.registerLogsReceivedForAuditloggTask(dbname, teknologi);
    }

    public List<AuditloggTaskDTO> getAuditloggTaskByNaisteam(String naisteam) {
        return adapter.getTasksRegisteredToNaisteam(naisteam);
    }

    public List<NaisTeamDTO> getAllTasksGroupedByNaisteam() {
        return adapter.findAllDistinctNaisteam().stream()
                .map(naisteam ->
                        NaisTeamDTO.builder()
                                .naisteam(naisteam)
                                .tasksForTeam(adapter.getTasksRegisteredToNaisteam(naisteam))
                                .build()).toList();
    }

    public List<NaisTeamDTO> getTasksGroupedByNaisteam(List<String> naisteams) {
        return naisteams.stream()
                .map(naisteam ->
                        NaisTeamDTO.builder()
                                .naisteam(naisteam)
                                .tasksForTeam(adapter.getTasksRegisteredToNaisteam(naisteam))
                                .build())
                .toList();
    }

    public boolean naisteamHasActiveAuditloggTasks(String naisteam) {
        return adapter.getTasksRegisteredToNaisteam(naisteam).stream()
                .anyMatch(task -> task.getFiksa() && (task.getLoggingLeseoperasjoner() || task.getLoggingEndringer()));
    }

    public List<String> findAllNaisteamWithActiveAuditloggTasks() {
        return adapter.findAllDistinctNaisteam().stream()
                .filter(this::naisteamHasActiveAuditloggTasks)
                .toList();
    }
}
