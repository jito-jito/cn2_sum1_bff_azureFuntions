package com.empresa.functions.usuarios;

import com.empresa.functions.common.exception.ValidationException;
import com.empresa.functions.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.functions.usuarios.dto.CrearUsuarioRequest;
import com.empresa.functions.usuarios.dto.UsuarioDto;
import java.util.List;

public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService() {
        this(new UsuarioRepository());
    }

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioDto crear(CrearUsuarioRequest request) {
        validar(request.username(), request.email());
        return repository.crear(request);
    }

    public List<UsuarioDto> listar() {
        return repository.listar();
    }

    public UsuarioDto obtener(Long id) {
        return repository.obtenerPorId(id);
    }

    public UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request) {
        validar(request.username(), request.email());
        return repository.actualizar(id, request);
    }

    public void eliminar(Long id) {
        repository.eliminarLogico(id);
    }

    public void asignarRol(Long usuarioId, Long rolId) {
        if (rolId == null) {
            throw new ValidationException("rolId es obligatorio");
        }
        repository.asignarRol(usuarioId, rolId);
    }

    public void quitarRol(Long usuarioId, Long rolId) {
        repository.quitarRol(usuarioId, rolId);
    }

    private void validar(String username, String email) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("username es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("email es obligatorio");
        }
    }
}
