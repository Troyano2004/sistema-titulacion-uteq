package com.erwin.backend.service;

import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.DocenteCarrera;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.DocenteCarreraRepository;
import com.erwin.backend.repository.DocenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Administra a qué carrera(s) pertenece un docente (tabla docente_carrera).
 * Hasta ahora esta tabla solo se LEÍA (comisión formativa, ejes temáticos,
 * banco de preguntas...); no existía ninguna pantalla para asignarla, así
 * que en la práctica quedaba vacía para cualquier docente nuevo.
 */
@Service
@Transactional
public class DocenteCarreraService {

    public record CarreraSimpleDto(Integer idCarrera, String nombre) {}
    public record DocenteConCarrerasDto(Integer idDocente, String nombreCompleto, String cedula,
                                         List<CarreraSimpleDto> carreras) {}

    private final DocenteRepository docenteRepo;
    private final CarreraRepository carreraRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;

    public DocenteCarreraService(DocenteRepository docenteRepo, CarreraRepository carreraRepo,
                                  DocenteCarreraRepository docenteCarreraRepo) {
        this.docenteRepo = docenteRepo;
        this.carreraRepo = carreraRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
    }

    @Transactional(readOnly = true)
    public List<CarreraSimpleDto> listarCarrerasDeDocente(Integer idDocente) {
        return docenteCarreraRepo.findByDocente_IdDocenteAndActivoTrue(idDocente).stream()
                .map(dc -> new CarreraSimpleDto(dc.getCarrera().getIdCarrera(), dc.getCarrera().getNombre()))
                .collect(Collectors.toList());
    }

    /** Todos los docentes con sus carreras ya agrupadas — para la pantalla de administración (evita N+1). */
    @Transactional(readOnly = true)
    public List<DocenteConCarrerasDto> listarTodosConCarreras() {
        Map<Integer, List<CarreraSimpleDto>> carrerasPorDocente = docenteCarreraRepo.findByActivoTrue().stream()
                .collect(Collectors.groupingBy(
                        dc -> dc.getDocente().getIdDocente(),
                        Collectors.mapping(
                                dc -> new CarreraSimpleDto(dc.getCarrera().getIdCarrera(), dc.getCarrera().getNombre()),
                                Collectors.toList())));

        return docenteRepo.findAll().stream()
                .map(d -> {
                    String nombre = d.getUsuario() != null
                            ? (d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()).trim()
                            : ("Docente " + d.getIdDocente());
                    String cedula = d.getUsuario() != null ? d.getUsuario().getCedula() : null;
                    List<CarreraSimpleDto> carreras = carrerasPorDocente.getOrDefault(d.getIdDocente(), List.of());
                    return new DocenteConCarrerasDto(d.getIdDocente(), nombre, cedula, carreras);
                })
                .sorted(Comparator.comparing(DocenteConCarrerasDto::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public void asignarCarrera(Integer idDocente, Integer idCarrera) {
        Docente docente = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + idDocente));
        Carrera carrera = carreraRepo.findById(idCarrera)
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + idCarrera));

        DocenteCarrera vinculo = docenteCarreraRepo.findByDocente_IdDocenteAndCarrera_IdCarrera(idDocente, idCarrera)
                .orElseGet(() -> {
                    DocenteCarrera nuevo = new DocenteCarrera();
                    nuevo.setDocente(docente);
                    nuevo.setCarrera(carrera);
                    return nuevo;
                });
        vinculo.setActivo(true);
        docenteCarreraRepo.save(vinculo);
    }

    public void quitarCarrera(Integer idDocente, Integer idCarrera) {
        docenteCarreraRepo.findByDocente_IdDocenteAndCarrera_IdCarrera(idDocente, idCarrera)
                .ifPresent(vinculo -> {
                    vinculo.setActivo(false);
                    docenteCarreraRepo.save(vinculo);
                });
    }
}
