package com.empresa.bff.roles.client;

import com.empresa.bff.roles.dto.ActualizarRolRequest;
import com.empresa.bff.roles.dto.CrearRolRequest;
import com.empresa.bff.roles.dto.RolDto;
import java.util.List;

public interface RolFunctionClient {

    RolDto crear(CrearRolRequest request);

    List<RolDto> listar();

    RolDto obtener(Long id);

    RolDto actualizar(Long id, ActualizarRolRequest request);

    void eliminar(Long id);
}
