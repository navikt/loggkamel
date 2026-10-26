package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.util.DB2AdminUsernameToTIdentConverter.ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS;
import static org.junit.jupiter.api.Assertions.*;

class DB2AdminUsernameToTIdentConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "dbAdmin1, dbAdmin1TIdent",
            "dbAdmin2, dbAdmin2TIdent",
            "dbAdmin3, dbAdmin3TIdent",
            "ordinaryTIdent, ordinaryTIdent",
            "dbAdminUnknown, dbAdminUnknown",
            "DBADMIN1, DBADMIN1"
    })
    void replaceWithTIdentIfIsAdminAuthId_mapsOnlyConfiguredAdmins(String loggedTIdent, String expectedTIdent) {
        DB2AdminUsernameToTIdentConverter resolver = new DB2AdminUsernameToTIdentConverter(objectMapper,
                "{\"dbAdmin1\":\"dbAdmin1TIdent\",\"dbAdmin2\":\"dbAdmin2TIdent\",\"dbAdmin3\":\"dbAdmin3TIdent\"}");

        assertEquals(expectedTIdent, resolver.replaceWithTIdentIfIsAdminAuthId(loggedTIdent));
    }

    @Test
    void replaceWithTIdentIfIsAdminAuthId_emptyMappingsPreserveIdentityIncludingNull() {
        DB2AdminUsernameToTIdentConverter resolver = new DB2AdminUsernameToTIdentConverter(objectMapper, "{}");

        assertEquals("ordinaryTIdent", resolver.replaceWithTIdentIfIsAdminAuthId("ordinaryTIdent"));
        assertNull(resolver.replaceWithTIdentIfIsAdminAuthId(null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", " ", "not json", "null", "[]", "\"text\"", "42",
            "{\"dbAdmin1\":null}", "{\"dbAdmin1\":42}", "{\"dbAdmin1\":true}",
            "{\"dbAdmin1\":{}}", "{\"dbAdmin1\":[]}", "{\"\":\"tIdent\"}",
            "{\" \":\"tIdent\"}", "{\"dbAdmin1\":\"\"}", "{\"dbAdmin1\":\"  \"}",
            "{\"dbAdmin1\":\"first\",\"dbAdmin1\":\"second\"}", "{} {}", "{} trailing"
    })
    void constructor_rejectsInvalidAdminMappingsWithoutExposingSecret(String json) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DB2AdminUsernameToTIdentConverter(objectMapper, json));

        assertEquals(ERROR_MESSAGE_INITIALIZING_ADMIN_MAPPINGS, exception.getMessage());
        assertNull(exception.getCause());
    }

}
