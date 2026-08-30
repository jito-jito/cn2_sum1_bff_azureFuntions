package com.empresa.bff.roles.service;

import com.empresa.bff.roles.client.RolFunctionClient;
import com.empresa.bff.roles.dto.ActualizarRolRequest;
import com.empresa.bff.roles.dto.CrearRolRequest;
import com.empresa.bff.roles.dto.RolDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RolService {

    private final RolFunctionClient client;

    public RolService(RolFunctionClient client) {
        this.client = client;
    }

    public RolDto crear(CrearRolRequest request) {
        return client.crear(request);
    }

    public List<RolDto> listar() {
        return client.listar();
    }

    public RolDto obtener(Long id) {
        return client.obtener(id);
    }

    public RolDto actualizar(Long id, ActualizarRolRequest request) {
        return client.actualizar(id, request);
    }

    public void eliminar(Long id) {
        client.eliminar(id);
    }
}
