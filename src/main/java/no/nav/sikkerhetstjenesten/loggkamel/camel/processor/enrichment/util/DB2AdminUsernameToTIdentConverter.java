package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DB2AdminUsernameToTIdentConverter {

    static final String ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS = "A secret named db2-admin-tidents must be present at startup and be a JSON object with unique, nonblank keys and nonblank string values";

    private final Map<String, String> db2AdminTIdents;

    public DB2AdminUsernameToTIdentConverter(ObjectMapper objectMapper, @Value("${db2-admin-tidents}") String db2AdminTIdentsJson) {
        this.db2AdminTIdents = initializeAdminUsernamesToTIdentsMap(objectMapper, db2AdminTIdentsJson);
    }

    private Map<String, String> initializeAdminUsernamesToTIdentsMap(ObjectMapper objectMapper, String adminToTIdentMappingsAsString) {
        if (adminToTIdentMappingsAsString == null || adminToTIdentMappingsAsString.isBlank()) {
            throw new IllegalArgumentException(ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS);
        }

        JsonNode adminToTIdentMappingsAsJson;
        try {
            adminToTIdentMappingsAsJson = objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(adminToTIdentMappingsAsString);
        } catch (JsonProcessingException e) {
            // Jackson errors can contain secret contents, so do not retain the cause.
            throw new IllegalArgumentException(ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS);
        }

        if (adminToTIdentMappingsAsJson == null || !adminToTIdentMappingsAsJson.isObject()) {
            throw new IllegalArgumentException(ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS);
        }

        Map<String, String> adminToTIdent = new HashMap<>();
        adminToTIdentMappingsAsJson.properties().forEach(entry -> {
            if (entry.getKey().isBlank() || !entry.getValue().isTextual() || entry.getValue().textValue().isBlank()) {
                throw new IllegalArgumentException(ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS);
            }
            adminToTIdent.put(entry.getKey(), entry.getValue().textValue());
        });
        return Map.copyOf(adminToTIdent);
    }

    public String replaceWithTIdentIfIsAdminAuthId(String loggedAuthId) {
        return loggedAuthId == null ? null : db2AdminTIdents.getOrDefault(loggedAuthId, loggedAuthId);
    }
}
