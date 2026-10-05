package com.empresa.functions.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.empresa.functions.auditoria.dto.EventoAuditoriaDto;
import com.empresa.functions.common.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditoriaServiceTest {

    private String entidadRecibida;
    private Long entidadIdRecibido;
    private int limitRecibido;

    private final AuditoriaService service = new AuditoriaService(new AuditoriaRepository() {
        @Override
        public List<EventoAuditoriaDto> listar(String entidad, Long entidadId, int limit) {
            entidadRecibida = entidad;
            entidadIdRecibido = entidadId;
            limitRecibido = limit;
            return List.of();
        }
    });

    @Test
    void normalizaFiltrosYAplicaLimitDefault() {
        service.listar("usuario", "42", null);

        assertEquals("USUARIO", entidadRecibida);
        assertEquals(42L, entidadIdRecibido);
        assertEquals(AuditoriaService.LIMIT_DEFAULT, limitRecibido);
    }

    @Test
    void sinFiltrosYLimitAcotadoAlMaximo() {
        service.listar(null, " ", "100000");

        assertNull(entidadRecibida);
        assertNull(entidadIdRecibido);
        assertEquals(AuditoriaService.LIMIT_MAX, limitRecibido);
    }

    @Test
    void rechazaEntidadDesconocidaYNumerosInvalidos() {
        assertThrows(ValidationException.class, () -> service.listar("PEDIDO", null, null));
        assertThrows(ValidationException.class, () -> service.listar(null, "abc", null));
        assertThrows(ValidationException.class, () -> service.listar(null, null, "diez"));
    }
}
