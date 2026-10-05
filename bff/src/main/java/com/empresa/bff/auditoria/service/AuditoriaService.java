package com.empresa.bff.auditoria.service;

import com.empresa.bff.auditoria.client.AuditoriaFunctionClient;
import com.empresa.bff.auditoria.dto.EventoAuditoriaDto;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Passthrough 1:1 hacia la Function de auditoría, igual que UsuarioService:
 * la validación de filtros vive en la Function (fuente de verdad de qué
 * entidades existen) y su 400 se propaga vía GlobalExceptionHandler.
 */
@Service
public class AuditoriaService {

    private final AuditoriaFunctionClient client;

    public AuditoriaService(AuditoriaFunctionClient client) {
        this.client = client;
    }

    public List<EventoAuditoriaDto> listar(String entidad, Long entidadId, Integer limit) {
        return client.listar(entidad, entidadId, limit);
    }
}
