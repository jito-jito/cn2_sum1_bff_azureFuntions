package com.empresa.functions.common.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventoRecibidoTest {

    /** Formato Event Grid Schema que entrega el @EventGridTrigger (un evento por invocación). */
    static final String EVENTO_USUARIO_ELIMINADO = """
            {
              "id": "6b9c1a2e-0000-4000-8000-000000000001",
              "topic": "/subscriptions/x/resourceGroups/rg-usuarios-roles/providers/Microsoft.EventGrid/topics/egt-usuarios-roles",
              "subject": "/usuarios/42",
              "eventType": "Usuarios.UsuarioEliminado",
              "eventTime": "2026-10-04T15:30:12.1234567Z",
              "data": {"entidad": "USUARIO", "entidadId": 42, "correlationId": "corr-abc", "payload": {"id": 42}},
              "dataVersion": "1.0",
              "metadataVersion": "1"
            }
            """;

    @Test
    void parseaEventoConDataEstandar() {
        EventoRecibido evento = EventoRecibido.parse(EVENTO_USUARIO_ELIMINADO);

        assertEquals("6b9c1a2e-0000-4000-8000-000000000001", evento.id());
        assertEquals(TiposEvento.USUARIO_ELIMINADO, evento.eventType());
        assertEquals("/usuarios/42", evento.subject());
        assertEquals("USUARIO", evento.entidad());
        assertEquals(42L, evento.entidadId());
        assertEquals("corr-abc", evento.correlationId());
        assertEquals(Instant.parse("2026-10-04T15:30:12.1234567Z"), evento.eventTimeInstant());
    }

    @Test
    void toleraEventosSinNuestraDataEstandar() {
        EventoRecibido evento = EventoRecibido.parse("""
                {"id": "1", "subject": "/x", "eventType": "Otro.Tipo", "data": "Hello World", "dataVersion": "0.1"}
                """);

        assertNull(evento.entidad());
        assertNull(evento.entidadId());
        assertNull(evento.correlationId());
    }

    @Test
    void jsonInvalidoEsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> EventoRecibido.parse("no-es-json"));
    }
}
