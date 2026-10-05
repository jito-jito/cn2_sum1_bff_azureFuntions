package com.empresa.functions.auditoria;

import com.empresa.functions.common.eventos.EventoRecibido;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

/**
 * Función consumidora de eventos: suscrita a todos los tipos del topic
 * (sub-auditoria, ver infra/eventgrid/setup-eventgrid.sh), registra cada
 * evento de dominio en AUDITORIA_EVENTOS como historial de cambios de
 * usuarios y roles, consultable vía ListarAuditoria / GET /auditoria del BFF.
 */
public class AuditoriaEventosFunction {

    private final AuditoriaService service = new AuditoriaService();

    @FunctionName("AuditarEvento")
    public void auditar(
            @EventGridTrigger(name = "evento") String content,
            ExecutionContext context) {
        EventoRecibido evento = EventoRecibido.parse(content);
        // Una excepción (ej. Oracle caído) hace fallar la invocación y Event
        // Grid reintenta según la retry policy de la suscripción.
        boolean registrado = service.registrar(evento);
        context.getLogger().info((registrado ? "Evento auditado: " : "Evento ya auditado (reentrega): ")
                + evento.eventType() + " " + evento.subject()
                + " [eventId=" + evento.id() + ", correlationId=" + evento.correlationId() + "]");
    }
}
