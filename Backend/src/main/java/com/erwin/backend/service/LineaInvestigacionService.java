package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.LineaInvestigacionRequest;
import com.erwin.backend.dtos.LineaInvestigacionResponse;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.LineaInvestigacion;
import com.erwin.backend.enums.EstadoLineaInvestigacion;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.LineaInvestigacionRepository;
import com.erwin.backend.security.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LineaInvestigacionService {

    private final LineaInvestigacionRepository lineaRepo;
    private final CarreraRepository carreraRepo;
    private final AuthorizationService authorizationService;

    public LineaInvestigacionService(LineaInvestigacionRepository lineaRepo, CarreraRepository carreraRepo,
                                      AuthorizationService authorizationService) {
        this.lineaRepo = lineaRepo;
        this.carreraRepo = carreraRepo;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<LineaInvestigacionResponse> listarPorCarrera(Integer idCarrera) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        return lineaRepo.findByCarrera_IdCarrera(idCarrera).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "LineaInvestigacion", accion = "CREATE")
    public LineaInvestigacionResponse crear(LineaInvestigacionRequest req) {
        authorizationService.exigirPropiaCarreraSiCoordinador(req.getIdCarrera());
        if (req.getNombre() == null || req.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la línea de investigación es obligatorio");
        }
        Carrera carrera = carreraRepo.findById(req.getIdCarrera())
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.getIdCarrera()));

        LineaInvestigacion linea = new LineaInvestigacion();
        linea.setCarrera(carrera);
        linea.setNombre(req.getNombre());
        linea.setDescripcion(req.getDescripcion());
        linea.setEstado(parseEstado(req.getEstado(), EstadoLineaInvestigacion.ACTIVA));
        linea.setFechaAprobacion(req.getFechaAprobacion());

        return convertirADto(lineaRepo.save(linea));
    }

    @Auditable(entidad = "LineaInvestigacion", accion = "UPDATE")
    public LineaInvestigacionResponse actualizar(Long id, LineaInvestigacionRequest req) {
        LineaInvestigacion linea = lineaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Línea de investigación no encontrada: " + id));
        if (linea.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(linea.getCarrera().getIdCarrera());
        }

        if (req.getIdCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(req.getIdCarrera());
            linea.setCarrera(carreraRepo.findById(req.getIdCarrera())
                    .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.getIdCarrera())));
        }
        if (req.getNombre() != null) linea.setNombre(req.getNombre());
        if (req.getDescripcion() != null) linea.setDescripcion(req.getDescripcion());
        if (req.getEstado() != null) linea.setEstado(parseEstado(req.getEstado(), linea.getEstado()));
        if (req.getFechaAprobacion() != null) linea.setFechaAprobacion(req.getFechaAprobacion());

        return convertirADto(lineaRepo.save(linea));
    }

    @Auditable(entidad = "LineaInvestigacion", accion = "DELETE")
    public void eliminar(Long id) {
        LineaInvestigacion linea = lineaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Línea de investigación no encontrada: " + id));
        if (linea.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(linea.getCarrera().getIdCarrera());
        }
        lineaRepo.deleteById(id);
    }

    private EstadoLineaInvestigacion parseEstado(String estado, EstadoLineaInvestigacion porDefecto) {
        if (estado == null || estado.isBlank()) return porDefecto;
        try {
            return EstadoLineaInvestigacion.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de línea de investigación inválido: " + estado);
        }
    }

    private LineaInvestigacionResponse convertirADto(LineaInvestigacion l) {
        return new LineaInvestigacionResponse(
                l.getId(),
                l.getCarrera() != null ? l.getCarrera().getIdCarrera() : null,
                l.getCarrera() != null ? l.getCarrera().getNombre() : null,
                l.getNombre(),
                l.getDescripcion(),
                l.getEstado() != null ? l.getEstado().name() : null,
                l.getFechaAprobacion()
        );
    }
}
