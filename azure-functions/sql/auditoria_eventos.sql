-- Auditoría de eventos de dominio (Exp3: arquitectura orientada a eventos).
-- La llena la función consumidora AuditarEvento (suscripción sub-auditoria
-- del Event Grid Topic) y la lee ListarAuditoria. Aplicar después de schema.sql.

CREATE TABLE AUDITORIA_EVENTOS (
    -- id que asigna Event Grid a cada evento: PK para que una reentrega del
    -- mismo evento (entrega at-least-once) no duplique la fila.
    EVENT_ID        VARCHAR2(64)    PRIMARY KEY,
    EVENT_TYPE      VARCHAR2(100)   NOT NULL,
    SUBJECT         VARCHAR2(200)   NOT NULL,
    ENTIDAD         VARCHAR2(30),
    ENTIDAD_ID      NUMBER,
    CORRELATION_ID  VARCHAR2(64),
    DATA            CLOB,
    EVENT_TIME      TIMESTAMP       NOT NULL,
    FECHA_REGISTRO  TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX IX_AUDITORIA_ENTIDAD ON AUDITORIA_EVENTOS (ENTIDAD, ENTIDAD_ID);
