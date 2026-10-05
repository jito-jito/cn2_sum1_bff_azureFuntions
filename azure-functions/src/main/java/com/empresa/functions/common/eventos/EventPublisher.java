package com.empresa.functions.common.eventos;

/**
 * Publica eventos de dominio. Interfaz para poder reemplazar Event Grid por
 * un fake en tests de los services sin depender del SDK ni de la red.
 */
public interface EventPublisher {

    /**
     * Best-effort: se llama después de que la escritura en Oracle ya se
     * confirmó, así que una falla al publicar no debe propagarse como error
     * de la operación HTTP (ver EventGridEventPublisher).
     */
    void publicar(EventoDominio evento);
}
