package com.empresa.functions.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.empresa.functions.common.eventos.EventPublisher;
import com.empresa.functions.common.eventos.EventoDominio;
import com.empresa.functions.common.eventos.TiposEvento;
import com.empresa.functions.common.exception.ConflictException;
import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.usuarios.dto.CrearUsuarioRequest;
import com.empresa.functions.usuarios.dto.UsuarioDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

    private final List<EventoDominio> publicados = new ArrayList<>();
    private final EventPublisher publisher = publicados::add;

    @Test
    void crearPublicaUsuarioCreadoConCorrelationId() {
        UsuarioRepository repo = new UsuarioRepository() {
            @Override
            public UsuarioDto crear(CrearUsuarioRequest request) {
                return new UsuarioDto(10L, request.username(), request.email(), request.nombreCompleto(), "ACTIVO",
                        List.of(), Instant.now(), null);
            }
        };
        UsuarioService service = new UsuarioService(repo, publisher);

        service.crear(new CrearUsuarioRequest("jperez", "jperez@empresa.cl", "Juan Pérez"), "corr-1");

        assertEquals(1, publicados.size());
        EventoDominio evento = publicados.get(0);
        assertEquals(TiposEvento.USUARIO_CREADO, evento.eventType());
        assertEquals("/usuarios/10", evento.subject());
        assertEquals(TiposEvento.ENTIDAD_USUARIO, evento.entidad());
        assertEquals(10L, evento.entidadId());
        assertEquals("corr-1", evento.correlationId());
    }

    @Test
    void siLaEscrituraFallaNoSePublica() {
        UsuarioRepository repo = new UsuarioRepository() {
            @Override
            public UsuarioDto crear(CrearUsuarioRequest request) {
                throw new ConflictException("duplicado");
            }

            @Override
            public void eliminarLogico(Long id) {
                throw new NotFoundException("no existe");
            }
        };
        UsuarioService service = new UsuarioService(repo, publisher);

        assertThrows(ConflictException.class,
                () -> service.crear(new CrearUsuarioRequest("jperez", "jperez@empresa.cl", null), "corr-2"));
        assertThrows(NotFoundException.class, () -> service.eliminar(99L, "corr-3"));
        assertTrue(publicados.isEmpty());
    }

    @Test
    void asignarRolPublicaConSubjectDeLaRelacion() {
        UsuarioRepository repo = new UsuarioRepository() {
            @Override
            public void asignarRol(Long usuarioId, Long rolId) {
                // ok
            }
        };
        UsuarioService service = new UsuarioService(repo, publisher);

        service.asignarRol(5L, 3L, "corr-4");

        assertEquals(TiposEvento.ROL_ASIGNADO, publicados.get(0).eventType());
        assertEquals("/usuarios/5/roles/3", publicados.get(0).subject());
        assertEquals(5L, publicados.get(0).entidadId());
    }
}
