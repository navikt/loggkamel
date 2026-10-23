package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.ColdStorageArchive;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2ColdStorageExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.ADMIN_AUTHORITY;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/auditlogg-backfills")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggBackfillController", description = "Administrerer backfill- og cold-storage-oppgaver")
public class AuditloggBackfillController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggBackfillController.class);

    private final AuditloggTaskService auditloggTaskService;
    private final DB2ColdStorageExportService coldStorageExportService;

    public AuditloggBackfillController(
            AuditloggTaskService auditloggTaskService,
            DB2ColdStorageExportService coldStorageExportService
    ) {
        this.auditloggTaskService = auditloggTaskService;
        this.coldStorageExportService = coldStorageExportService;
    }

    @Secured(ADMIN_AUTHORITY)
    @GetMapping("/requested")
    @Operation(summary = "Henter alle backfill- og cold-storage-oppgaver som venter på behandling")
    public List<NaisteamDTO> getRequestedTasks() {
        return auditloggTaskService.findRequestedBackfillTasksGroupedByNaisteam();
    }

    @Secured(ADMIN_AUTHORITY)
    @GetMapping("/db2/{dbName}")
    @Operation(summary = "Laster ned DB2-logger for cold storage")
    public ResponseEntity<StreamingResponseBody> downloadDB2ColdStorageExport(
            @PathVariable("dbName") String dbName
    ) {
        ColdStorageArchive archive = coldStorageExportService.createExport(dbName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(archive.downloadFilename()).build());
        headers.setCacheControl("no-store");

        StreamingResponseBody responseBody = outputStream -> {
            try (archive) {
                Files.copy(archive.archivePath(), outputStream);
                log.info("Delivered DB2 cold-storage export {}: {} lines, {} monthly archives, generated in {} ms",
                        dbName, archive.lineCount(), archive.monthlyArchiveCount(), archive.generationTimeMillis());
            } catch (IOException exception) {
                log.error("Failed to deliver DB2 cold-storage export for database {} after generating {} lines",
                        dbName, archive.lineCount());
                throw exception;
            }
        };
        return ResponseEntity.ok().headers(headers).body(responseBody);
    }

    @Secured(ADMIN_AUTHORITY)
    @PostMapping("/{teknologi}/{dbName}/complete")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Markerer en backfill- eller cold-storage-oppgave som fullført")
    public void completeRequestedTask(
            @PathVariable("teknologi") TeknologiEnum teknoloji,
            @PathVariable("dbName") String dbName
    ) {
        auditloggTaskService.completeRequestedTask(dbName, teknoloji);
    }
}
