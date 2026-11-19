package com.erwin.backend.service;

import com.erwin.backend.dtos.ListadoSeleccionesResponse;
import com.erwin.backend.dtos.ListadoSeleccionesResponse.EstudianteSeleccionDto;
import com.erwin.backend.dtos.ListadoSeleccionesResponse.GrupoOpcionDto;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.EleccionTitulacion;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.EleccionTitulacionRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import com.erwin.backend.util.NivelAcademicoUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Listado preliminar (Módulo 4) de estudiantes del antepenúltimo nivel de una
 * carrera con su opción de titulación seleccionada (o "SIN_SELECCIONAR"),
 * agrupado por opción — insumo que se envía a la Coordinación de Carrera.
 */
@Service
@Transactional(readOnly = true)
public class TutoriaListadoService {

    private static final String SIN_SELECCIONAR = "SIN_SELECCIONAR";

    private final CarreraRepository carreraRepo;
    private final EstudianteRepository estudianteRepo;
    private final EleccionTitulacionRepository eleccionRepo;
    private final PeriodoTitulacionRepository periodoRepo;

    public TutoriaListadoService(CarreraRepository carreraRepo,
                                  EstudianteRepository estudianteRepo,
                                  EleccionTitulacionRepository eleccionRepo,
                                  PeriodoTitulacionRepository periodoRepo) {
        this.carreraRepo = carreraRepo;
        this.estudianteRepo = estudianteRepo;
        this.eleccionRepo = eleccionRepo;
        this.periodoRepo = periodoRepo;
    }

    public ListadoSeleccionesResponse listado(Integer idCarrera, String periodo) {
        Carrera carrera = carreraRepo.findById(idCarrera)
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + idCarrera));
        int antepenultimo = NivelAcademicoUtil.antepenultimo(carrera);

        List<Estudiante> estudiantes = estudianteRepo.findByCarrera_IdCarrera(idCarrera).stream()
                .filter(e -> e.getNivelSemestre() != null && e.getNivelSemestre() == antepenultimo)
                .toList();

        Map<Integer, EleccionTitulacion> eleccionPorEstudiante = new HashMap<>();
        if (periodo != null && !periodo.isBlank()) {
            periodoRepo.findByDescripcion(periodo).ifPresent(p ->
                    eleccionRepo.findByCarrera_IdCarreraAndPeriodo_IdPeriodo(idCarrera, p.getIdPeriodo())
                            .forEach(el -> eleccionPorEstudiante.put(el.getEstudiante().getIdEstudiante(), el)));
        }

        Map<String, List<EstudianteSeleccionDto>> agrupado = new LinkedHashMap<>();
        for (Estudiante e : estudiantes) {
            EleccionTitulacion eleccion = eleccionPorEstudiante.get(e.getIdEstudiante());
            String opcion = (eleccion != null && eleccion.getModalidad() != null)
                    ? eleccion.getModalidad().getNombre() : SIN_SELECCIONAR;

            EstudianteSeleccionDto dto = new EstudianteSeleccionDto(
                    e.getIdEstudiante(),
                    nombreCompleto(e),
                    e.getUsuario() != null ? e.getUsuario().getCedula() : null,
                    opcion);

            agrupado.computeIfAbsent(opcion, k -> new ArrayList<>()).add(dto);
        }

        List<GrupoOpcionDto> grupos = agrupado.entrySet().stream()
                .map(entry -> new GrupoOpcionDto(entry.getKey(), entry.getValue()))
                .toList();

        return new ListadoSeleccionesResponse(idCarrera, carrera.getNombre(), periodo, antepenultimo, grupos);
    }

    private String nombreCompleto(Estudiante e) {
        if (e.getUsuario() == null) return "";
        String nombres = e.getUsuario().getNombres() != null ? e.getUsuario().getNombres() : "";
        String apellidos = e.getUsuario().getApellidos() != null ? e.getUsuario().getApellidos() : "";
        return (nombres + " " + apellidos).trim();
    }
}
