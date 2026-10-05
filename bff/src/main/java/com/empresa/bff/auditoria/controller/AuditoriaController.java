package com.empresa.bff.auditoria.controller;

import com.empresa.bff.auditoria.dto.EventoAuditoriaDto;
import com.empresa.bff.auditoria.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    /** Ej: GET /auditoria?entidad=USUARIO&entidadId=42 — historial de eventos de ese usuario. */
    @GetMapping
    public List<EventoAuditoriaDto> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) Integer limit) {
        return service.listar(entidad, entidadId, limit);
    }
}
