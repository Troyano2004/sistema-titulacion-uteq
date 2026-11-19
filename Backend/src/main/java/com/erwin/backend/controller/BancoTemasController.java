package com.erwin.backend.controller;

import com.erwin.backend.dtos.AprobarConsejoDirectivoRequest;
import com.erwin.backend.dtos.BancoTemaResponse;
import com.erwin.backend.entities.BancoTemas;
import com.erwin.backend.service.BancoTemasConsejoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/banco-temas")
@CrossOrigin(origins = "http://localhost:4200")
public class BancoTemasController {

    private final BancoTemasConsejoService service;

    public BancoTemasController(BancoTemasConsejoService service) {
        this.service = service;
    }

    @PutMapping("/{id}/aprobar-consejo-directivo")
    @PreAuthorize("hasAnyRole('DECANO','COORDINADOR')")
    public ResponseEntity<BancoTemaResponse> aprobarConsejoDirectivo(
            @PathVariable Integer id, @RequestBody AprobarConsejoDirectivoRequest req) {
        return ResponseEntity.ok(toDto(service.aprobarConsejoDirectivo(id, req)));
    }

    private BancoTemaResponse toDto(BancoTemas t) {
        return new BancoTemaResponse(
                t.getIdTema(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getCarrera() != null ? t.getCarrera().getNombre() : null,
                t.getDocenteProponente() != null && t.getDocenteProponente().getUsuario() != null
                        ? (t.getDocenteProponente().getUsuario().getNombres() + " " + t.getDocenteProponente().getUsuario().getApellidos()).trim()
                        : null,
                t.getEstado() != null ? t.getEstado().name() : null,
                t.getObservaciones(),
                t.getEstudianteSugerente() != null ? t.getEstudianteSugerente().getIdEstudiante() : null,
                t.getAprobadoPorConsejo() != null
                        ? (t.getAprobadoPorConsejo().getNombres() + " " + t.getAprobadoPorConsejo().getApellidos()).trim()
                        : null,
                t.getFechaAprobacionConsejo()
        );
    }
}
