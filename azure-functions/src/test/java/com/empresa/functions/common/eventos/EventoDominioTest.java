package com.empresa.functions.common.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.empresa.functions.roles.dto.RolDto;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventoDominioTest {

    @Test
    void armaEventGridEventConDataEstandarYFechasIso() {
        RolDto rol = new RolDto(7L, "ADMIN", "Administrador", "ACTIVO", Instant.parse("2026-10-04T12:00:00Z"));
        EventoDominio evento = new EventoDominio(TiposEvento.ROL_CREADO, "/roles/7", TiposEvento.ENTIDAD_ROL, 7L,
                "corr-123", rol);

        EventGridEvent egEvent = evento.toEventGridEvent();

        assertEquals("Roles.RolCreado", egEvent.getEventType());
        assertEquals("/roles/7", egEvent.getSubject());
        assertEquals(EventoDominio.DATA_VERSION, egEvent.getDataVersion());

        Map<?, ?> data = egEvent.getData().toObject(Map.class);
        assertEquals("ROL", data.get("entidad"));
        assertEquals(7, ((Number) data.get("entidadId")).intValue());
        assertEquals("corr-123", data.get("correlationId"));
        Map<?, ?> payload = (Map<?, ?>) data.get("payload");
        assertEquals("ADMIN", payload.get("nombre"));
        assertEquals("2026-10-04T12:00:00Z", payload.get("fechaCreacion"));
    }
}
