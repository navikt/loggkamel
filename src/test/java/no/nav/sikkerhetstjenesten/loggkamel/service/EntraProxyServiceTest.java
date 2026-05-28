package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraProxyServiceTest {

    private static final String NAV_IDENT = "A123456";
    private static final String T_IDENT = "T654321";

    @Mock
    EntraProxyAdapter adapter;

    @InjectMocks
    EntraProxyService service;

    @Test
    void getAnsattFraNavIdent_successful() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().navIdent(NAV_IDENT).epost("a@nav.no").build();
        when(adapter.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(ansatt);

        assertEquals(ansatt, service.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraNavIdent_exceptionPassesThrough() {
        when(adapter.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> service.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraTIdent_successful() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().tident(T_IDENT).epost("t@nav.no").build();
        when(adapter.getAnsattFraTIdent(T_IDENT)).thenReturn(ansatt);

        assertEquals(ansatt, service.getAnsattFraTIdent(T_IDENT));
    }

    @Test
    void getAnsattFraTIdent_exceptionPassesThrough() {
        when(adapter.getAnsattFraTIdent(T_IDENT)).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> service.getAnsattFraTIdent(T_IDENT));
    }

}

