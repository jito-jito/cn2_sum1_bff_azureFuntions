package com.empresa.functions.roles;

import com.empresa.functions.common.ValidationUtil;
import com.empresa.functions.common.eventos.EventGridEventPublisher;
import com.empresa.functions.common.eventos.EventPublisher;
import com.empresa.functions.common.eventos.EventoDominio;
import com.empresa.functions.common.eventos.TiposEvento;
import com.empresa.functions.roles.dto.ActualizarRolRequest;
import com.empresa.functions.roles.dto.CrearRolRequest;
import com.empresa.functions.roles.dto.RolDto;
import java.util.List;
import java.util.Map;

/**
 * Además del CRUD, es la función generadora de eventos del dominio roles
 * (mismo patrón que UsuarioService).
 */
public class RolService {

    private final RolRepository repository;
    private final EventPublisher eventos;

    public RolService() {
        this(new RolRepository(), EventGridEventPublisher.get());
    }

    public RolService(RolRepository repository, EventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public RolDto crear(CrearRolRequest request, String correlationId) {
        ValidationUtil.validate(request);
        RolDto creado = repository.crear(request);
        publicar(TiposEvento.ROL_CREADO, creado.id(), correlationId, creado);
        return creado;
    }

    public List<RolDto> listar() {
        return repository.listar();
    }

    public RolDto obtener(Long id) {
        return repository.obtenerPorId(id);
    }

    public RolDto actualizar(Long id, ActualizarRolRequest request, String correlationId) {
        ValidationUtil.validate(request);
        RolDto actualizado = repository.actualizar(id, request);
        publicar(TiposEvento.ROL_ACTUALIZADO, id, correlationId, actualizado);
        return actualizado;
    }

    public void eliminar(Long id, String correlationId) {
        repository.eliminarLogico(id);
        publicar(TiposEvento.ROL_ELIMINADO, id, correlationId, Map.of("id", id));
    }

    private void publicar(String tipo, Long rolId, String correlationId, Object payload) {
        eventos.publicar(new EventoDominio(tipo, "/roles/" + rolId, TiposEvento.ENTIDAD_ROL, rolId, correlationId,
                payload));
    }
}
