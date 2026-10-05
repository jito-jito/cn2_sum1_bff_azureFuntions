package com.empresa.functions.common.eventos;

/**
 * Catálogo de eventos de dominio publicados al Event Grid Topic. El prefijo
 * ("Usuarios." / "Roles.") es el dominio que lo emite; las suscripciones
 * filtran por estos valores exactos (ver infra/eventgrid/setup-eventgrid.sh),
 * así que renombrar uno es un cambio de contrato.
 */
public final class TiposEvento {

    public static final String USUARIO_CREADO = "Usuarios.UsuarioCreado";
    public static final String USUARIO_ACTUALIZADO = "Usuarios.UsuarioActualizado";
    public static final String USUARIO_ELIMINADO = "Usuarios.UsuarioEliminado";
    public static final String ROL_ASIGNADO = "Usuarios.RolAsignado";
    public static final String ROL_QUITADO = "Usuarios.RolQuitado";

    public static final String ROL_CREADO = "Roles.RolCreado";
    public static final String ROL_ACTUALIZADO = "Roles.RolActualizado";
    public static final String ROL_ELIMINADO = "Roles.RolEliminado";

    public static final String ENTIDAD_USUARIO = "USUARIO";
    public static final String ENTIDAD_ROL = "ROL";

    private TiposEvento() {
    }
}
