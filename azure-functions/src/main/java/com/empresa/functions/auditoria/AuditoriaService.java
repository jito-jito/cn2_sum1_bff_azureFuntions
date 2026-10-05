package com.empresa.functions.auditoria;

import com.empresa.functions.auditoria.dto.EventoAuditoriaDto;
import com.empresa.functions.common.eventos.EventoRecibido;
import com.empresa.functions.common.eventos.TiposEvento;
import com.empresa.functions.common.exception.ValidationException;
import java.util.List;
import java.util.Set;

public class AuditoriaService {

    static final int LIMIT_DEFAULT = 50;
    static final int LIMIT_MAX = 200;

    private static final Set<String> ENTIDADES = Set.of(TiposEvento.ENTIDAD_USUARIO, TiposEvento.ENTIDAD_ROL);

    private final AuditoriaRepository repository;

    public AuditoriaService() {
        this(new AuditoriaRepository());
    }

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    public boolean registrar(EventoRecibido evento) {
        return repository.registrar(evento);
    }

    /** Los parámetros llegan crudos desde el query string; null = sin filtro. */
    public List<EventoAuditoriaDto> listar(String entidad, String entidadId, String limit) {
        String entidadNormalizada = entidad == null || entidad.isBlank() ? null : entidad.trim().toUpperCase();
        if (entidadNormalizada != null && !ENTIDADES.contains(entidadNormalizada)) {
            throw new ValidationException("entidad: debe ser uno de " + ENTIDADES);
        }
        Long id = parseLong(entidadId, "entidadId");
        Long limite = parseLong(limit, "limit");
        int limiteEfectivo = limite == null ? LIMIT_DEFAULT : (int) Math.min(Math.max(limite, 1), LIMIT_MAX);
        return repository.listar(entidadNormalizada, id, limiteEfectivo);
    }

    private Long parseLong(String raw, String campo) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(campo + ": debe ser numérico");
        }
    }
}
