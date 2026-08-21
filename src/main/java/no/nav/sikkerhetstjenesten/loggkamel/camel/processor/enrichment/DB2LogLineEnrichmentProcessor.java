package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidDB2LogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class DB2LogLineEnrichmentProcessor extends NativeLogLineEnrichmentProcessor {

    private final ObjectMapper objectMapper;

    @Autowired
    public DB2LogLineEnrichmentProcessor(Metrics metrics, EntraProxyService entraProxyService,  ObjectMapper objectMapper, Validator validator) {
        super(metrics, entraProxyService, validator);
        this.objectMapper = objectMapper;
    }

    public void enrich(Exchange exchange) {
        AuditloggLineMessage auditloggLineMessage = exchange.getMessage().getBody(AuditloggLineMessage.class);

        if (auditloggLineMessage == null || auditloggLineMessage.getBody().isBlank()) {
            throw new InvalidDB2LogLineException("Audit log message is blank");
        }

        DB2AuditloggLineDTO bodyAsDTO;
        try {
            bodyAsDTO = objectMapper.readValue(auditloggLineMessage.getBody(), DB2AuditloggLineDTO.class);
        } catch (JsonProcessingException e) {
            throw new InvalidDB2LogLineException("Failure assigning serialized DB2AuditloggLineDTO to appropriate class", e);
        }

        String pgCommand;
        EnrichedAuditlogg.AuditClass pgAuditClass;

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(bodyAsDTO.getSqlQuery());

            switch (statement) {
                case Select s      -> { pgAuditClass = EnrichedAuditlogg.AuditClass.READ;  pgCommand = "SELECT"; }
                case Insert i      -> { pgAuditClass = EnrichedAuditlogg.AuditClass.WRITE; pgCommand = "INSERT"; }
                case Update u      -> { pgAuditClass = EnrichedAuditlogg.AuditClass.WRITE; pgCommand = "UPDATE"; }
                case Delete d      -> { pgAuditClass = EnrichedAuditlogg.AuditClass.WRITE; pgCommand = "DELETE"; }
                case Merge m       -> { pgAuditClass = EnrichedAuditlogg.AuditClass.WRITE; pgCommand = "MERGE"; }
                case Execute e     -> { pgAuditClass = EnrichedAuditlogg.AuditClass.FUNCTION; pgCommand = "EXECUTE"; }
                case Grant g       -> { pgAuditClass = EnrichedAuditlogg.AuditClass.ROLE; pgCommand = "GRANT"; }
                case CreateTable c -> { pgAuditClass = EnrichedAuditlogg.AuditClass.DDL; pgCommand = "CREATE TABLE"; }
                case Alter a       -> { pgAuditClass = EnrichedAuditlogg.AuditClass.DDL; pgCommand = "ALTER TABLE"; }
                case Drop d        -> { pgAuditClass = EnrichedAuditlogg.AuditClass.DDL; pgCommand = "DROP " + d.getType(); }
                case SetStatement s-> { pgAuditClass = EnrichedAuditlogg.AuditClass.MISC_SET; pgCommand = "SET"; }
                default            -> {
                    pgAuditClass = EnrichedAuditlogg.AuditClass.MISC;
                    pgCommand = statement.getClass().getSimpleName().toUpperCase();
                    log.warn("DB2 statement parsing of uncategorized statement type {}", pgCommand);
                    metrics.incrementDB2Issue(Metrics.DB2IssueType.unexpectedStatementType);
                }
            }
        } catch (JSQLParserException e) {
            log.warn("Failed to parse SQL statement for DB2 Log Packet {}, placeInPacket {}. Likely due to DB2-idiosyncratic syntax.",
                    exchange.getMessage().getHeader(FILE_NAME, String.class), auditloggLineMessage.getHeader().getPlaceInPacket(), e);
            metrics.incrementDB2Issue(Metrics.DB2IssueType.unparsable);

            // For sql statements that we cannot parse, treat them as endringer (the type that must get logged for økonomisystemer)
            pgAuditClass = EnrichedAuditlogg.AuditClass.WRITE;
            pgCommand = "UNKNOWN";
        }

        EnrichedAuditlogg enrichedAuditlogg = EnrichedAuditlogg.builder()
                .originalMessage(auditloggLineMessage.getBody())
                .sqlStatement(bodyAsDTO.getSqlQuery())
                .logTime(bodyAsDTO.getMetricsTimestamp().atZone(ZoneId.systemDefault())) //TODO: test that this is being set to CET/CEST
                .navIdent(bodyAsDTO.getAuthId())
                .dbName(bodyAsDTO.getDatabaseName())
                .pgAuditClass(pgAuditClass)
                .auditType(EnrichedAuditlogg.AuditType.SESSION) // Hardcoded to session as it most closely matches the logging done by DB2 SYSTOOLS
                .pgCommand(pgCommand)
                .build();

        enrichedAuditlogg.setEpost(getAnsattEpost(enrichedAuditlogg.getNavIdent()));
        validateEnrichedAuditlogg(enrichedAuditlogg);
        exchange.getMessage().setBody(enrichedAuditlogg);
    }
}
