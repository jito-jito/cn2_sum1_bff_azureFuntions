package com.empresa.bff.usuarios.client;

import com.empresa.bff.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.bff.usuarios.dto.CrearUsuarioRequest;
import com.empresa.bff.usuarios.dto.UsuarioDto;
import java.util.List;

/**
 * Cliente hacia la Azure Function de usuarios. Una interfaz por dominio de
 * Function, para poder mockearla en tests del BFF sin levantar la Function.
 */
public interface UsuarioFunctionClient {

    UsuarioDto crear(CrearUsuarioRequest request);

    List<UsuarioDto> listar();

    UsuarioDto obtener(Long id);

    UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request);

    void eliminar(Long id);

    void asignarRol(Long usuarioId, Long rolId);

    void quitarRol(Long usuarioId, Long rolId);
}
