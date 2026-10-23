package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.ColdStorageArchive;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2ColdStorageExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.ADMIN_AUTHORITY;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.USER_AUTHORITY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditloggBackfillControllerTest {

    private static final String DB_NAME = "db-a";

    @Mock
    AuditloggTaskService auditloggTaskService;

    @Mock
    DB2ColdStorageExportService coldStorageExportService;

    @Mock
    OAuth2AuthenticatedPrincipal principal;

    @TempDir
    Path temporaryDirectory;

    private AuditloggBackfillController controller() {
        ProxyFactory factory = new ProxyFactory(new AuditloggBackfillController(auditloggTaskService, coldStorageExportService));
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.secured());
        return (AuditloggBackfillController) factory.getProxy();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRequestedTasksReturnsRequestedTasksForAdmin() {
        authenticateAsAdmin();
        List<NaisteamDTO> tasks = List.of(NaisteamDTO.builder().naisteam("team-a").tasksForTeam(List.of()).build());
        when(auditloggTaskService.findRequestedBackfillTasksGroupedByNaisteam()).thenReturn(tasks);

        assertEquals(tasks, controller().getRequestedTasks());
    }

    @Test
    void completeRequestedTaskUpdatesTheTaskForAdmin() {
        authenticateAsAdmin();

        controller().completeRequestedTask(TeknologiEnum.POSTGRESQL, DB_NAME);

        verify(auditloggTaskService).completeRequestedTask(DB_NAME, TeknologiEnum.POSTGRESQL);
    }

    @Test
    void downloadDB2ColdStorageExportStreamsArchiveAndCleansTemporaryFiles() throws Exception {
        authenticateAsAdmin();
        Path archivePath = temporaryDirectory.resolve("archive.zip");
        Files.writeString(archivePath, "zip-content");
        ColdStorageArchive archive = new ColdStorageArchive(
                archivePath, temporaryDirectory, "db-a.cold-storage.zip", 5, 1, 23);
        when(coldStorageExportService.createExport(DB_NAME)).thenReturn(archive);

        var response = controller().downloadDB2ColdStorageExport(DB_NAME);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamingResponseBody responseBody = response.getBody();
        assertNotNull(responseBody);
        responseBody.writeTo(outputStream);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        assertEquals("zip-content", outputStream.toString());
        assertFalse(Files.exists(temporaryDirectory));
    }

    @Test
    void regularUserCannotCallAnyAdminEndpoint() {
        authenticateAsUser();
        AuditloggBackfillController securedController = controller();

        assertThrows(AccessDeniedException.class, securedController::getRequestedTasks);
        assertThrows(AccessDeniedException.class,
                () -> securedController.downloadDB2ColdStorageExport(DB_NAME));
        assertThrows(AccessDeniedException.class,
                () -> securedController.completeRequestedTask(TeknologiEnum.DB2, DB_NAME));
        verifyNoInteractions(auditloggTaskService, coldStorageExportService);
    }

    @Test
    void unauthenticatedCallerCannotCallAnyAdminEndpoint() {
        SecurityContextHolder.clearContext();
        AuditloggBackfillController securedController = controller();

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                securedController::getRequestedTasks);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> securedController.downloadDB2ColdStorageExport(DB_NAME));
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> securedController.completeRequestedTask(TeknologiEnum.DB2, DB_NAME));
        verifyNoInteractions(auditloggTaskService, coldStorageExportService);
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority(USER_AUTHORITY), new SimpleGrantedAuthority(ADMIN_AUTHORITY))));
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority(USER_AUTHORITY))));
    }
}
