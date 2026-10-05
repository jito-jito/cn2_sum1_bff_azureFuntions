package com.empresa.functions.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.azure.functions.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class UsuarioEventosFunctionTest {

    private final List<Long> usuariosProcesados = new ArrayList<>();

    private final UsuarioRepository repo = new UsuarioRepository() {
        @Override
        public int quitarRolesDeUsuarioInactivo(Long usuarioId) {
            usuariosProcesados.add(usuarioId);
            return 2;
        }
    };

    private final ExecutionContext context = new ExecutionContext() {
        @Override
        public Logger getLogger() {
            return Logger.getLogger("test");
        }

        @Override
        public String getInvocationId() {
            return "inv-1";
        }

        @Override
        public String getFunctionName() {
            return "QuitarRolesDeUsuarioEliminado";
        }
    };

    @Test
    void usuarioEliminadoQuitaSusRoles() {
        new UsuarioEventosFunction(repo).quitarRolesDeUsuarioEliminado(evento("Usuarios.UsuarioEliminado"), context);

        assertEquals(List.of(42L), usuariosProcesados);
    }

    @Test
    void otrosTiposSeIgnoran() {
        new UsuarioEventosFunction(repo).quitarRolesDeUsuarioEliminado(evento("Usuarios.UsuarioCreado"), context);

        assertTrue(usuariosProcesados.isEmpty());
    }

    private static String evento(String tipo) {
        return """
                {"id": "e-1", "subject": "/usuarios/42", "eventType": "%s", "eventTime": "2026-10-04T15:30:12Z",
                 "data": {"entidad": "USUARIO", "entidadId": 42, "correlationId": "c-1"}, "dataVersion": "1.0"}
                """.formatted(tipo);
    }
}
