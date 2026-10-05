package com.empresa.functions.usuarios;

import com.empresa.functions.common.eventos.EventoRecibido;
import com.empresa.functions.common.eventos.TiposEvento;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import java.util.logging.Logger;

/**
 * Función consumidora de eventos del dominio usuarios. Suscrita al topic con
 * filtro por tipo "Usuarios.UsuarioEliminado" (sub-cascada-usuario-eliminado,
 * ver infra/eventgrid/setup-eventgrid.sh): al eliminar un usuario, quita sus
 * roles de forma asíncrona, fuera del request HTTP que lo eliminó.
 */
public class UsuarioEventosFunction {

    private final UsuarioRepository repository;

    public UsuarioEventosFunction() {
        this(new UsuarioRepository());
    }

    UsuarioEventosFunction(UsuarioRepository repository) {
        this.repository = repository;
    }

    @FunctionName("QuitarRolesDeUsuarioEliminado")
    public void quitarRolesDeUsuarioEliminado(
            @EventGridTrigger(name = "evento") String content,
            ExecutionContext context) {
        Logger log = context.getLogger();
        EventoRecibido evento = EventoRecibido.parse(content);

        // La suscripción ya filtra por tipo; se valida igual por si alguien la
        // reconfigura sin filtro (procesar otro tipo aquí sería un bug).
        if (!TiposEvento.USUARIO_ELIMINADO.equals(evento.eventType())) {
            log.warning("Evento ignorado por QuitarRolesDeUsuarioEliminado: " + evento.eventType());
            return;
        }
        Long usuarioId = evento.entidadId();
        if (usuarioId == null) {
            log.warning("Evento " + evento.id() + " sin entidadId, se descarta");
            return;
        }

        // Una excepción aquí hace fallar la invocación y Event Grid reintenta
        // (y tras agotar los intentos, va al container de dead-letter).
        int quitados = repository.quitarRolesDeUsuarioInactivo(usuarioId);
        log.info("Usuario " + usuarioId + " eliminado: " + quitados + " rol(es) quitado(s) [eventId="
                + evento.id() + ", correlationId=" + evento.correlationId() + "]");
    }
}
