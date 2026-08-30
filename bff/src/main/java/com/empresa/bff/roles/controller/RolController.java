package com.empresa.bff.roles.controller;

import com.empresa.bff.roles.dto.ActualizarRolRequest;
import com.empresa.bff.roles.dto.CrearRolRequest;
import com.empresa.bff.roles.dto.RolDto;
import com.empresa.bff.roles.service.RolService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
public class RolController {

    private final RolService service;

    public RolController(RolService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RolDto> crear(@Valid @RequestBody CrearRolRequest request) {
        RolDto creado = service.crear(request);
        return ResponseEntity.created(URI.create("/roles/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<RolDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public RolDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public RolDto actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarRolRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
