package com.empresa.functions.roles;

import com.empresa.functions.common.exception.ValidationException;
import com.empresa.functions.roles.dto.ActualizarRolRequest;
import com.empresa.functions.roles.dto.CrearRolRequest;
import com.empresa.functions.roles.dto.RolDto;
import java.util.List;

public class RolService {

    private final RolRepository repository;

    public RolService() {
        this(new RolRepository());
    }

    public RolService(RolRepository repository) {
        this.repository = repository;
    }

    public RolDto crear(CrearRolRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new ValidationException("nombre es obligatorio");
        }
        return repository.crear(request);
    }

    public List<RolDto> listar() {
        return repository.listar();
    }

    public RolDto obtener(Long id) {
        return repository.obtenerPorId(id);
    }

    public RolDto actualizar(Long id, ActualizarRolRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new ValidationException("nombre es obligatorio");
        }
        return repository.actualizar(id, request);
    }

    public void eliminar(Long id) {
        repository.eliminarLogico(id);
    }
}
