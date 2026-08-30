package com.empresa.bff.usuarios.controller;

import com.empresa.bff.usuarios.dto.ActualizarUsuarioRequest;
import com.empresa.bff.usuarios.dto.AsignarRolRequest;
import com.empresa.bff.usuarios.dto.CrearUsuarioRequest;
import com.empresa.bff.usuarios.dto.UsuarioDto;
import com.empresa.bff.usuarios.service.UsuarioService;
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
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioDto creado = service.crear(request);
        return ResponseEntity.created(URI.create("/usuarios/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<Void> asignarRol(@PathVariable Long id, @Valid @RequestBody AsignarRolRequest request) {
        service.asignarRol(id, request.rolId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/roles/{rolId}")
    public ResponseEntity<Void> quitarRol(@PathVariable Long id, @PathVariable Long rolId) {
        service.quitarRol(id, rolId);
        return ResponseEntity.noContent().build();
    }
}
