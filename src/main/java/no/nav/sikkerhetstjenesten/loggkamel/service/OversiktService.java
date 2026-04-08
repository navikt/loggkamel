package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
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

    public AuditLoggArkivResponseDTO createAuditLoggArkiv(AuditLoggArkivRequestDTO request) {
        return adapter.createAuditLoggArkiv(request);
    }

    public AuditLoggArkivResponseDTO updateAuditLoggArkiv(AuditLoggArkivRequestDTO request) {
        return adapter.updateAuditLoggArkiv(request);
    }

    public AuditLoggArkivResponseDTO getAuditLoggArkivByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public List<AuditLoggArkivResponseDTO> getAuditLoggArkivByNaisteam(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam);
    }
}
