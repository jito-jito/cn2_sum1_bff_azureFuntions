package com.empresa.functions.auditoria;

import com.empresa.functions.auditoria.dto.EventoAuditoriaDto;
import com.empresa.functions.common.DataSourceProvider;
import com.empresa.functions.common.JsonUtil;
import com.empresa.functions.common.eventos.EventoRecibido;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    private static final int ORA_UNIQUE_CONSTRAINT_VIOLATED = 1;

    /**
     * Registra un evento recibido. Devuelve false si ya estaba registrado:
     * Event Grid garantiza entrega at-least-once, así que una reentrega del
     * mismo evento (mismo id) es un caso normal, no un error.
     */
    public boolean registrar(EventoRecibido evento) {
        String sql = "INSERT INTO AUDITORIA_EVENTOS (EVENT_ID, EVENT_TYPE, SUBJECT, ENTIDAD, ENTIDAD_ID, "
                + "CORRELATION_ID, DATA, EVENT_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, evento.id());
            ps.setString(2, evento.eventType());
            ps.setString(3, evento.subject());
            ps.setString(4, evento.entidad());
            if (evento.entidadId() == null) {
                ps.setNull(5, Types.NUMERIC);
            } else {
                ps.setLong(5, evento.entidadId());
            }
            ps.setString(6, evento.correlationId());
            ps.setString(7, evento.data() == null ? null : evento.data().toString());
            ps.setTimestamp(8, Timestamp.from(evento.eventTimeInstant()));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_UNIQUE_CONSTRAINT_VIOLATED) {
                return false;
            }
            throw new RuntimeException("Error al registrar evento de auditoría " + evento.id(), e);
        }
    }

    public List<EventoAuditoriaDto> listar(String entidad, Long entidadId, int limit) {
        StringBuilder sql = new StringBuilder("SELECT EVENT_ID, EVENT_TYPE, SUBJECT, ENTIDAD, ENTIDAD_ID, "
                + "CORRELATION_ID, DATA, EVENT_TIME, FECHA_REGISTRO FROM AUDITORIA_EVENTOS WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (entidad != null) {
            sql.append(" AND ENTIDAD = ?");
            params.add(entidad);
        }
        if (entidadId != null) {
            sql.append(" AND ENTIDAD_ID = ?");
            params.add(entidadId);
        }
        sql.append(" ORDER BY EVENT_TIME DESC FETCH FIRST ? ROWS ONLY");
        params.add(limit);

        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<EventoAuditoriaDto> eventos = new ArrayList<>();
                while (rs.next()) {
                    eventos.add(map(rs));
                }
                return eventos;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar auditoría de eventos", e);
        }
    }

    private EventoAuditoriaDto map(ResultSet rs) throws SQLException {
        long entidadId = rs.getLong("ENTIDAD_ID");
        return new EventoAuditoriaDto(
                rs.getString("EVENT_ID"),
                rs.getString("EVENT_TYPE"),
                rs.getString("SUBJECT"),
                rs.getString("ENTIDAD"),
                rs.wasNull() ? null : entidadId,
                rs.getString("CORRELATION_ID"),
                parseData(rs.getString("DATA")),
                rs.getTimestamp("EVENT_TIME").toInstant(),
                rs.getTimestamp("FECHA_REGISTRO").toInstant());
    }

    private JsonNode parseData(String data) {
        if (data == null) {
            return null;
        }
        try {
            return JsonUtil.mapper().readTree(data);
        } catch (JsonProcessingException e) {
            // Se guardó desde un JsonNode, así que no debería pasar; si pasa,
            // se devuelve el texto crudo en vez de romper el listado completo.
            return JsonUtil.mapper().getNodeFactory().textNode(data);
        }
    }
}
