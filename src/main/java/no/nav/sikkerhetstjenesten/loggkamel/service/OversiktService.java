package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
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

    public AuditloggArkivResponseDTO createAuditloggArkiv(AuditloggArkivRequestDTO request) {
        return adapter.createAuditloggArkiv(request);
    }

    public AuditloggArkivResponseDTO updateAuditloggArkiv(AuditloggArkivRequestDTO request) {
        return adapter.updateAuditloggArkiv(request);
    }

    public AuditloggArkivResponseDTO getAuditloggArkivByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public void registerLogsReceivedForAuditloggArkiv(String dbname, TeknologiEnum teknologi) {
        adapter.registerLogsReceivedForAuditloggArkiv(dbname, teknologi);
    }

    public List<AuditloggArkivResponseDTO> getAuditloggArkivByNaisteam(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam);
    }
}
