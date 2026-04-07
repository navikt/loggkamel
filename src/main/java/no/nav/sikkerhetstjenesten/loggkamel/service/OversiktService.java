package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
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

    public AuditLoggArkivDTO createAuditLoggArkiv(AuditLoggArkivDTO request) {
        return adapter.createAuditLoggArkiv(request);
    }

    public AuditLoggArkivDTO updateAuditLoggArkiv(AuditLoggArkivDTO request) {
        return adapter.updateAuditLoggArkiv(request);
    }

    public AuditLoggArkivDTO getAuditLoggArkivByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public List<AuditLoggArkivDTO> getAuditLoggArkivByNaisteam(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam);
    }
}
