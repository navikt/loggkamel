package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public AuditloggTaskDTO updateAuditloggTask(AuditloggTaskRequestDTO targetStateForAuditloggTaskToUpdate, List<String> naisteamsForUser) {
        requireMembership(targetStateForAuditloggTaskToUpdate.getNaisteam(), naisteamsForUser);

        AuditloggTaskDTO existingTaskToBeUpdated = adapter.findByDbnameAndTeknologi(targetStateForAuditloggTaskToUpdate.getDbname(), targetStateForAuditloggTaskToUpdate.getTeknologi());
        if (existingTaskToBeUpdated != null) {
            requireMembership(existingTaskToBeUpdated.getNaisteam(), naisteamsForUser);
        }

        return adapter.updateAuditloggTask(targetStateForAuditloggTaskToUpdate);
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

    public List<NaisteamDTO> getAllTasksGroupedByNaisteam() {
        return adapter.findAllDistinctNaisteam().stream()
                .map(naisteam ->
                        NaisteamDTO.builder()
                                .naisteam(naisteam)
                                .tasksForTeam(adapter.getTasksRegisteredToNaisteam(naisteam))
                                .build()).toList();
    }

    public List<NaisteamDTO> getTasksGroupedByNaisteam(List<String> naisteams) {
        return naisteams.stream()
                .map(naisteam ->
                        NaisteamDTO.builder()
                                .naisteam(naisteam)
                                .tasksForTeam(adapter.getTasksRegisteredToNaisteam(naisteam))
                                .build())
                .toList();
    }

    public boolean naisteamHasActiveAuditloggTasks(String naisteam) {
        return adapter.getTasksRegisteredToNaisteam(naisteam).stream()
                .anyMatch(task -> task.getFiksa() && (task.getLoggingLeseoperasjoner() || task.getLoggingEndringer()));
    }

    public List<AuditloggTaskDTO> findActiveTasksByTeknologi(TeknologiEnum teknologi) {
        return adapter.findConfiguredTasksByTeknologi(teknologi).stream()
                .filter(this::taskHasAnActiveForwardingCategory)
                .toList();
    }

    public List<AuditloggTaskDTO> findActiveTasksByTeknologiAndBackfillStatus(
            TeknologiEnum teknologi, BackfillStatus backfillStatus) {
        return adapter.findConfiguredTasksByTeknologiAndBackfill(teknologi, backfillStatus).stream()
                .filter(this::taskHasAnActiveForwardingCategory)
                .toList();
    }

    public void setBackfillStatus(
            String dbname, TeknologiEnum teknologi, BackfillStatus backfillStatus) {
        adapter.setBackfillStatus(dbname, teknologi, backfillStatus);
    }

    public List<NaisteamDTO> findRequestedBackfillTasksGroupedByNaisteam() {
        Map<String, List<AuditloggTaskDTO>> naisteamToTasksItOwns = new LinkedHashMap<>();

        List<AuditloggTaskDTO> allTasksInRequestedState = adapter.findTasksByBackfillStatuses(List.of(
                BackfillStatus.BACKFILL_REQUESTED,
                BackfillStatus.COLD_STORAGE_REQUESTED));
        allTasksInRequestedState
                .forEach(task -> naisteamToTasksItOwns
                        .computeIfAbsent(task.getNaisteam() == null ? "unassigned" : task.getNaisteam(), ignored -> new ArrayList<>())
                        .add(task));

        return naisteamToTasksItOwns.entrySet().stream()
                .map(entry -> NaisteamDTO.builder()
                        .naisteam(entry.getKey())
                        .tasksForTeam(entry.getValue())
                        .build())
                .toList();
    }

    public void completeRequestedTask(String dbname, TeknologiEnum teknologi) {
        AuditloggTaskDTO task = adapter.findByDbnameAndTeknologi(dbname, teknologi);
        if (task == null) {
            throw new AuditloggTaskNotFoundException(
                    "No auditlogg task exists for database " + dbname + " and technology " + teknologi);
        }
        if (task.getBackfill() == null) {
            throw invalidRequestedStatus(dbname, teknologi, null);
        }

        BackfillStatus completedStatus = switch (teknologi) {
            case DB2 -> {
                requireRequestedStatus(task.getBackfill(), dbname, teknologi, BackfillStatus.COLD_STORAGE_REQUESTED);
                yield BackfillStatus.COLD_STORAGE_COMPLETED;
            }
            case POSTGRESQL -> switch (task.getBackfill()) {
                case COLD_STORAGE_REQUESTED -> BackfillStatus.COLD_STORAGE_COMPLETED;
                case BACKFILL_REQUESTED -> BackfillStatus.BACKFILL_COMPLETED;
                default -> throw invalidRequestedStatus(dbname, teknologi, task.getBackfill());
            };
            default -> throw new InvalidAuditloggTaskStateException(
                    "Completing backfill requests is not supported for technology " + teknologi);
        };

        adapter.setBackfillStatus(dbname, teknologi, completedStatus);
    }

    private static void requireRequestedStatus(
            BackfillStatus currentStatus,
            String dbname,
            TeknologiEnum teknoloji,
            BackfillStatus requiredStatus
    ) {
        if (currentStatus != requiredStatus) {
            throw invalidRequestedStatus(dbname, teknoloji, currentStatus);
        }
    }

    private static InvalidAuditloggTaskStateException invalidRequestedStatus(
            String dbname, TeknologiEnum teknoloji, BackfillStatus currentStatus) {
        return new InvalidAuditloggTaskStateException(
                "Task for database " + dbname + " and technology " + teknoloji
                        + " is not in a completable requested state (current state: " + currentStatus + ")");
    }

    private boolean taskHasAnActiveForwardingCategory(AuditloggTaskDTO task) {
        return !task.getDiscardLogs() && (task.getLoggingLeseoperasjoner() || task.getLoggingEndringer());
    }

    public List<String> findAllNaisteamWithActiveAuditloggTasks() {
        return adapter.findAllDistinctNaisteam().stream()
                .filter(this::naisteamHasActiveAuditloggTasks)
                .toList();
    }
}
