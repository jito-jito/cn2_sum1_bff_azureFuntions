package com.empresa.functions.usuarios;

import com.empresa.functions.common.DataSourceProvider;
import com.empresa.functions.common.exception.ConflictException;
import com.empresa.functions.common.exception.NotFoundException;
import com.empresa.functions.roles.dto.RolDto;
import com.empresa.functions.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.functions.usuarios.dto.CrearUsuarioRequest;
import com.empresa.functions.usuarios.dto.UsuarioDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UsuarioRepository {

    private static final int ORA_UNIQUE_CONSTRAINT_VIOLATED = 1;

    public UsuarioDto crear(CrearUsuarioRequest request) {
        String sql = "INSERT INTO USUARIOS (USERNAME, EMAIL, NOMBRE_COMPLETO) VALUES (?, ?, ?)";
        long id;
        try (Connection conn = DataSourceProvider.get().getConnection();
                // Oracle JDBC: pedir la columna por nombre es necesario para que
                // getGeneratedKeys() devuelva el valor del IDENTITY y no un ROWID.
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "ID" })) {
            ps.setString(1, request.username());
            ps.setString(2, request.email());
            ps.setString(3, request.nombreCompleto());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
        // Fuera del try-with-resources: obtenerPorId abre su propia conexión y
        // no debe hacerlo mientras la de arriba sigue abierta (ver §note pool).
        return obtenerPorId(id);
    }

    public List<UsuarioDto> listar() {
        String sql = "SELECT ID, USERNAME, EMAIL, NOMBRE_COMPLETO, ESTADO, FECHA_CREACION, FECHA_MODIF "
                + "FROM USUARIOS ORDER BY ID";
        List<UsuarioDto> usuarios = new ArrayList<>();
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(map(rs, List.of()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios", e);
        }
        if (usuarios.isEmpty()) {
            return usuarios;
        }
        // rolesDeUsuarios abre su propia conexión: se llama recién aquí, con la
        // de arriba ya cerrada, para no necesitar 2 conexiones simultáneas por
        // invocación (con la latencia hacia la ADB, la 2ª conexión concurrente
        // podía superar el connectionTimeout de HikariCP).
        Map<Long, List<RolDto>> rolesPorUsuario = rolesDeUsuarios(
                usuarios.stream().map(UsuarioDto::id).collect(Collectors.toList()));
        return usuarios.stream()
                .map(u -> reemplazarRoles(u, rolesPorUsuario.getOrDefault(u.id(), List.of())))
                .toList();
    }

    public UsuarioDto obtenerPorId(Long id) {
        String sql = "SELECT ID, USERNAME, EMAIL, NOMBRE_COMPLETO, ESTADO, FECHA_CREACION, FECHA_MODIF "
                + "FROM USUARIOS WHERE ID = ?";
        UsuarioDto usuario;
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("Usuario " + id + " no encontrado");
                }
                usuario = map(rs, List.of());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener usuario " + id, e);
        }
        return reemplazarRoles(usuario, rolesDeUsuario(id));
    }

    public UsuarioDto actualizar(Long id, ActualizarUsuarioRequest request) {
        String sql = "UPDATE USUARIOS SET USERNAME = ?, EMAIL = ?, NOMBRE_COMPLETO = ?, FECHA_MODIF = SYSTIMESTAMP "
                + "WHERE ID = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.username());
            ps.setString(2, request.email());
            ps.setString(3, request.nombreCompleto());
            ps.setLong(4, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("Usuario " + id + " no encontrado");
            }
        } catch (SQLException e) {
            throw translate(e);
        }
        return obtenerPorId(id);
    }

    public void eliminarLogico(Long id) {
        String sql = "UPDATE USUARIOS SET ESTADO = 'INACTIVO', FECHA_MODIF = SYSTIMESTAMP "
                + "WHERE ID = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("Usuario " + id + " no encontrado");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar usuario " + id, e);
        }
    }

    public void asignarRol(Long usuarioId, Long rolId) {
        verificarActivo("USUARIOS", usuarioId, "Usuario");
        verificarActivo("ROLES", rolId, "Rol");
        String sql = "INSERT INTO USUARIO_ROL (USUARIO_ID, ROL_ID) VALUES (?, ?)";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, rolId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_UNIQUE_CONSTRAINT_VIOLATED) {
                return; // el rol ya estaba asignado: operación idempotente
            }
            throw new RuntimeException("Error al asignar rol", e);
        }
    }

    public void quitarRol(Long usuarioId, Long rolId) {
        String sql = "DELETE FROM USUARIO_ROL WHERE USUARIO_ID = ? AND ROL_ID = ?";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, rolId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("El usuario " + usuarioId + " no tiene asignado el rol " + rolId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al quitar rol", e);
        }
    }

    private void verificarActivo(String tabla, Long id, String etiqueta) {
        String sql = "SELECT 1 FROM " + tabla + " WHERE ID = ? AND ESTADO = 'ACTIVO'";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException(etiqueta + " " + id + " no encontrado o inactivo");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar " + etiqueta.toLowerCase(), e);
        }
    }

    private List<RolDto> rolesDeUsuario(Long usuarioId) {
        return rolesDeUsuarios(List.of(usuarioId)).getOrDefault(usuarioId, List.of());
    }

    private Map<Long, List<RolDto>> rolesDeUsuarios(List<Long> usuarioIds) {
        if (usuarioIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = usuarioIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT UR.USUARIO_ID, R.ID, R.NOMBRE, R.DESCRIPCION, R.ESTADO, R.FECHA_CREACION "
                + "FROM USUARIO_ROL UR JOIN ROLES R ON R.ID = UR.ROL_ID "
                + "WHERE UR.USUARIO_ID IN (" + placeholders + ")";
        try (Connection conn = DataSourceProvider.get().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < usuarioIds.size(); i++) {
                ps.setLong(i + 1, usuarioIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, List<RolDto>> result = new HashMap<>();
                while (rs.next()) {
                    Long usuarioId = rs.getLong("USUARIO_ID");
                    RolDto rol = new RolDto(
                            rs.getLong("ID"),
                            rs.getString("NOMBRE"),
                            rs.getString("DESCRIPCION"),
                            rs.getString("ESTADO"),
                            rs.getTimestamp("FECHA_CREACION").toInstant());
                    result.computeIfAbsent(usuarioId, k -> new ArrayList<>()).add(rol);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener roles de usuarios", e);
        }
    }

    private UsuarioDto map(ResultSet rs, List<RolDto> roles) throws SQLException {
        return new UsuarioDto(
                rs.getLong("ID"),
                rs.getString("USERNAME"),
                rs.getString("EMAIL"),
                rs.getString("NOMBRE_COMPLETO"),
                rs.getString("ESTADO"),
                roles,
                rs.getTimestamp("FECHA_CREACION").toInstant(),
                rs.getTimestamp("FECHA_MODIF") == null ? null : rs.getTimestamp("FECHA_MODIF").toInstant());
    }

    private UsuarioDto reemplazarRoles(UsuarioDto usuario, List<RolDto> roles) {
        return new UsuarioDto(usuario.id(), usuario.username(), usuario.email(), usuario.nombreCompleto(),
                usuario.estado(), roles, usuario.fechaCreacion(), usuario.fechaModificacion());
    }

    private RuntimeException translate(SQLException e) {
        if (e.getErrorCode() == ORA_UNIQUE_CONSTRAINT_VIOLATED) {
            return new ConflictException("Ya existe un usuario con ese username o email");
        }
        return new RuntimeException("Error de base de datos", e);
    }
}
