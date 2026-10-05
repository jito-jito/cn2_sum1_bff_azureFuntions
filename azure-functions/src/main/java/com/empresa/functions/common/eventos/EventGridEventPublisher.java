package com.empresa.functions.common.eventos;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publicador hacia el Event Grid Topic como singleton estático: el cliente
 * del SDK (pool HTTP de Netty incluido) se construye una sola vez y se
 * reutiliza entre invocaciones "calientes", mismo motivo que
 * DataSourceProvider. Endpoint y key vienen de App Settings, nunca del código.
 *
 * Publicación best-effort post-commit: si Event Grid falla, se loguea y la
 * operación CRUD responde igual (el dato ya quedó en Oracle). El costo es
 * que ese evento se pierde para la auditoría; un outbox transaccional lo
 * resolvería y queda como mejora futura (ver CLAUDE.md, sección Event Grid).
 */
public final class EventGridEventPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventGridEventPublisher.class.getName());

    private static volatile EventPublisher instance;

    private final EventGridPublisherClient<EventGridEvent> client;

    EventGridEventPublisher(EventGridPublisherClient<EventGridEvent> client) {
        this.client = client;
    }

    public static EventPublisher get() {
        EventPublisher result = instance;
        if (result == null) {
            synchronized (EventGridEventPublisher.class) {
                result = instance;
                if (result == null) {
                    instance = result = build();
                }
            }
        }
        return result;
    }

    private static EventPublisher build() {
        String endpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
        String key = System.getenv("EVENTGRID_TOPIC_KEY");
        if (isBlank(endpoint) || isBlank(key)) {
            // Dev local sin topic: el CRUD sigue funcionando y los eventos
            // quedan visibles en el log en vez de fallar cada escritura.
            LOG.warning("EVENTGRID_TOPIC_ENDPOINT/EVENTGRID_TOPIC_KEY sin configurar: los eventos solo se loguean");
            return evento -> LOG.info("Evento no publicado (Event Grid sin configurar): "
                    + evento.eventType() + " " + evento.subject());
        }
        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildEventGridEventPublisherClient();
        return new EventGridEventPublisher(client);
    }

    @Override
    public void publicar(EventoDominio evento) {
        try {
            client.sendEvent(evento.toEventGridEvent());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo publicar el evento " + evento.eventType() + " " + evento.subject()
                    + " (correlationId=" + evento.correlationId() + ")", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
