package com.empresa.functions.roles;

import com.empresa.functions.common.DataSourceProvider;
import com.empresa.functions.common.exception.ConflictException;
import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.roles.dto.ActualizarRolRequest;
import com.empresa.functions.roles.dto.CrearRolRequest;
import com.empresa.functions.roles.dto.RolDto;
import com.empresa.functions.roles.dto.UsuarioResumenDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolRepository {

    private static final int ORA_UNIQUE_CONSTRAINT_VIOLATED = 1;

    public RolDto crear(CrearRolRequest request) {
        String sql = "INSERT INTO ROLES (NOMBRE, DESCRIPCION) VALUES (?, ?)";
        try (Connection conn = DataSourceProvider.get().getConnection();
                // Oracle JDBC: pedir la columna por nombre es necesario para que
                // getGeneratedKeys() devuelva el valor del IDENTITY y no un ROWID.
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "ID" })) {
            ps.setString(1, request.nombre());
            ps.setString(2, request.descripcion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return obtenerPorId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public List<RolDto> listar() {
        String sql = "SELECT ID, NOMBRE, DESCRIPCION, ESTADO, FECHA_CREACION FROM ROLES ORDER BY ID";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<RolDto> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(map(rs));
            }
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar roles", e);
        }
    }

    public RolDto obtenerPorId(Long id) {
        String sql = "SELECT ID, NOMBRE, DESCRIPCION, ESTADO, FECHA_CREACION FROM ROLES WHERE ID = ?";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("Rol " + id + " no encontrado");
                }
                return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener rol " + id, e);
        }
    }

    public RolDto actualizar(Long id, ActualizarRolRequest request) {
        String sql = "UPDATE ROLES SET NOMBRE = ?, DESCRIPCION = ? WHERE ID = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.nombre());
            ps.setString(2, request.descripcion());
            ps.setLong(3, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("Rol " + id + " no encontrado");
            }
            return obtenerPorId(id);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public void eliminarLogico(Long id) {
        if (tieneUsuariosAsignados(id)) {
            throw new ConflictException("El rol " + id + " tiene usuarios asignados, no se puede eliminar");
        }
        String sql = "UPDATE ROLES SET ESTADO = 'INACTIVO' WHERE ID = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("Rol " + id + " no encontrado");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar rol " + id, e);
        }
    }

    /** Relacion inversa de USUARIO_ROL, usada por la capa GraphQL (Rol.usuarios). */
    public List<UsuarioResumenDto> usuariosDelRol(Long rolId) {
        String sql = "SELECT U.ID, U.USERNAME, U.EMAIL, U.ESTADO "
                + "FROM USUARIO_ROL UR JOIN USUARIOS U ON U.ID = UR.USUARIO_ID "
                + "WHERE UR.ROL_ID = ? ORDER BY U.ID";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                List<UsuarioResumenDto> usuarios = new ArrayList<>();
                while (rs.next()) {
                    usuarios.add(new UsuarioResumenDto(
                            rs.getLong("ID"), rs.getString("USERNAME"),
                            rs.getString("EMAIL"), rs.getString("ESTADO")));
                }
                return usuarios;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuarios del rol " + rolId, e);
        }
    }

    private boolean tieneUsuariosAsignados(Long rolId) {
        String sql = "SELECT COUNT(*) FROM USUARIO_ROL WHERE ROL_ID = ?";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar uso del rol " + rolId, e);
        }
    }

    private RolDto map(ResultSet rs) throws SQLException {
        return new RolDto(
                rs.getLong("ID"),
                rs.getString("NOMBRE"),
                rs.getString("DESCRIPCION"),
                rs.getString("ESTADO"),
                rs.getTimestamp("FECHA_CREACION").toInstant());
    }

    private RuntimeException translate(SQLException e) {
        if (e.getErrorCode() == ORA_UNIQUE_CONSTRAINT_VIOLATED) {
            return new ConflictException("Ya existe un rol con ese nombre");
        }
        return new RuntimeException("Error de base de datos", e);
    }
}
