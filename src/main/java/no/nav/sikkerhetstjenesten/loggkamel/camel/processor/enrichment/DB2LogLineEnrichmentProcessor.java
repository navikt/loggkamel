package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

@Service
public class DB2LogLineEnrichmentProcessor extends NativeLogLineEnrichmentProcessor {

    private final ObjectMapper objectMapper;

    @Autowired
    public DB2LogLineEnrichmentProcessor(Metrics metrics, EntraProxyService entraProxyService,  ObjectMapper objectMapper, Validator validator) {
        super(metrics, entraProxyService, validator);
        this.objectMapper = objectMapper;
    }

    public void enrich(Exchange exchange) {
        String bodyAsString = exchange.getMessage().getBody(AuditloggLineMessage.class).getBody();

        if (bodyAsString == null || bodyAsString.isBlank()) {
            throw new InvalidDB2LogLineException("Audit log message is blank");
        }

        DB2AuditloggLineDTO bodyAsDTO;
        try {
            bodyAsDTO = objectMapper.readValue(bodyAsString, DB2AuditloggLineDTO.class);
        } catch (JsonProcessingException e) {
            throw new InvalidDB2LogLineException("Failure assigning serialized DB2AuditloggLineDTO to appropriate class", e);
        }

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(bodyAsDTO.getSqlQuery());
        } catch (JSQLParserException e) {
            throw new InvalidDB2LogLineException("Failure when parsing DB2 log line as SQL statement", e);
        }

        String pgCommand;
        EnrichedAuditlogg.AuditClass pgAuditClass;

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
                log.info("DB2 statement parsing of uncategorized statement type {}", pgCommand);
                metrics.incrementMiscDB2StatementType();
            }
        }

        EnrichedAuditlogg enrichedAuditlogg = EnrichedAuditlogg.builder()
                .originalMessage(bodyAsString)
                .sqlStatement(bodyAsDTO.getSqlQuery())
                .logTime(bodyAsDTO.getMetricsTimestamp().atZone(ZoneId.systemDefault())) //TODO: test that this is being set to CET/CEST
                .navIdent(bodyAsDTO.getAuthId())
                .dbName(bodyAsDTO.getDatabaseName())
                .pgAuditClass(pgAuditClass)
                .auditType(EnrichedAuditlogg.AuditType.SESSION) // Hardcoded to session as it most closely matches the logging done by DB2 SYSTOOLS
                .pgCommand(pgCommand)
                .build();

        enrichedAuditlogg.setEpost(getAnsattEpost(enrichedAuditlogg.getNavIdent()));
        exchange.getMessage().setBody(enrichedAuditlogg);
    }
}
