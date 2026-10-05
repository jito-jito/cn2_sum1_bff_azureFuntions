package com.empresa.bff.auditoria.client;

import com.empresa.bff.auditoria.dto.EventoAuditoriaDto;
import java.util.List;

/**
 * Cliente hacia la Azure Function ListarAuditoria (historial de eventos de
 * dominio que registra la consumidora de Event Grid AuditarEvento).
 */
public interface AuditoriaFunctionClient {

    List<EventoAuditoriaDto> listar(String entidad, Long entidadId, Integer limit);
}
