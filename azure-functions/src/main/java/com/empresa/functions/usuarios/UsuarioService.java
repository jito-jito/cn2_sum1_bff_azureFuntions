package com.empresa.functions.usuarios;

import com.empresa.functions.common.ValidationUtil;
import com.empresa.functions.common.eventos.EventGridEventPublisher;
import com.empresa.functions.common.eventos.EventPublisher;
import com.empresa.functions.common.eventos.EventoDominio;
import com.empresa.functions.common.eventos.TiposEvento;
import com.empresa.functions.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.functions.usuarios.dto.CrearUsuarioRequest;
import com.empresa.functions.usuarios.dto.UsuarioDto;
import java.util.List;
import java.util.Map;

/**
 * Además del CRUD, es la función generadora de eventos del dominio usuarios:
 * cada escritura exitosa publica su evento al Event Grid Topic. Si la
 * escritura falla (excepción del repository), no se publica nada.
 */
public class UsuarioService {

    private static final String SUBJECT_BASE = "/usuarios/";

    private final UsuarioRepository repository;
    private final EventPublisher eventos;

    public UsuarioService() {
        this(new UsuarioRepository(), EventGridEventPublisher.get());
    }

    public UsuarioService(UsuarioRepository repository, EventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public UsuarioDto crear(CrearUsuarioRequest request, String correlationId) {
        ValidationUtil.validate(request);
        UsuarioDto creado = repository.crear(request);
        publicar(TiposEvento.USUARIO_CREADO, SUBJECT_BASE + creado.id(), creado.id(), correlationId, creado);
        return creado;
    }

    public List<UsuarioDto> listar() {
        return repository.listar();
    }

    public UsuarioDto obtener(Long id) {
        return repository.obtenerPorId(id);
    }

    public UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request, String correlationId) {
        ValidationUtil.validate(request);
        UsuarioDto actualizado = repository.actualizar(id, request);
        publicar(TiposEvento.USUARIO_ACTUALIZADO, SUBJECT_BASE + id, id, correlationId, actualizado);
        return actualizado;
    }

    public void eliminar(Long id, String correlationId) {
        repository.eliminarLogico(id);
        publicar(TiposEvento.USUARIO_ELIMINADO, SUBJECT_BASE + id, id, correlationId, Map.of("id", id));
    }

    public void asignarRol(Long usuarioId, Long rolId, String correlationId) {
        repository.asignarRol(usuarioId, rolId);
        publicar(TiposEvento.ROL_ASIGNADO, SUBJECT_BASE + usuarioId + "/roles/" + rolId, usuarioId, correlationId,
                Map.of("usuarioId", usuarioId, "rolId", rolId));
    }

    public void quitarRol(Long usuarioId, Long rolId, String correlationId) {
        repository.quitarRol(usuarioId, rolId);
        publicar(TiposEvento.ROL_QUITADO, SUBJECT_BASE + usuarioId + "/roles/" + rolId, usuarioId, correlationId,
                Map.of("usuarioId", usuarioId, "rolId", rolId));
    }

    private void publicar(String tipo, String subject, Long usuarioId, String correlationId, Object payload) {
        eventos.publicar(new EventoDominio(tipo, subject, TiposEvento.ENTIDAD_USUARIO, usuarioId, correlationId,
                payload));
    }
}
