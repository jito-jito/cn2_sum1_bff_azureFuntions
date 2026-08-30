package com.empresa.bff.usuarios.service;

import com.empresa.bff.usuarios.client.UsuarioFunctionClient;
import com.empresa.bff.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.bff.usuarios.dto.CrearUsuarioRequest;
import com.empresa.bff.usuarios.dto.UsuarioDto;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Orquesta las llamadas a la Azure Function de usuarios. Hoy es un
 * passthrough 1:1 porque el contrato de la Function ya calza con el DTO
 * expuesto por el BFF; si más adelante se necesita agregar datos de otra
 * Function, esta es la capa donde se combina esa respuesta.
 */
@Service
public class UsuarioService {

    private final UsuarioFunctionClient client;

    public UsuarioService(UsuarioFunctionClient client) {
        this.client = client;
    }

    public UsuarioDto crear(CrearUsuarioRequest request) {
        return client.crear(request);
    }

    public List<UsuarioDto> listar() {
        return client.listar();
    }

    public UsuarioDto obtener(Long id) {
        return client.obtener(id);
    }

    public UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request) {
        return client.actualizar(id, request);
    }

    public void eliminar(Long id) {
        client.eliminar(id);
    }

    public void asignarRol(Long usuarioId, Long rolId) {
        client.asignarRol(usuarioId, rolId);
    }

    public void quitarRol(Long usuarioId, Long rolId) {
        client.quitarRol(usuarioId, rolId);
    }
}
