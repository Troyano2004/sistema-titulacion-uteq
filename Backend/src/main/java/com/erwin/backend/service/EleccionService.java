package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.entities.EleccionTitulacion;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.enums.EstadoEleccion;
import com.erwin.backend.repository.CarreraModalidadRepository;
import com.erwin.backend.repository.EleccionTitulacionRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Punto único de escritura para la modalidad de titulación que un estudiante
 * elige por periodo ({@link EleccionTitulacion}). Es el ancla que usa todo el
 * flujo banco-de-temas → propuesta → anteproyecto → proyecto (ver
 * ComisionTemasController), y aquí es donde se hace efectivo el conteo de
 * cambios (numeroCambios/motivoUltimoCambio/fechaUltimoCambio) que la entidad
 * ya tenía declarado pero que nunca se actualizaba.
 *
 * NOTA — EleccionTitulacion vs MatriculaTitulacion: no son redundantes.
 * EleccionTitulacion es la referencia (estudiante, periodo) → modalidad que
 * sostiene la cadena de FKs de PropuestaTitulacion/AnteproyectoTitulacion/
 * ProyectoTitulacion (NOT NULL en las tres). MatriculaTitulacion es el
 * registro formal de matrícula de Titulación I/II exigido por el reglamento
 * (Art. 14-18): valida nivel académico, prerrequisitos, y controla el
 * cambio de opción con límite y plazo. Unificarlas implicaría reescribir esas
 * tres entidades y todos sus servicios/controladores — fuera del alcance
 * quirúrgico de esta tarea y con alto riesgo de romper el pipeline de temas
 * ya en producción.
 */
@Service
public class EleccionService {

    private final EleccionTitulacionRepository repo;
    private final CarreraModalidadRepository carreraModalidadRepo;
    private final EstudianteRepository estudianteRepo;
    private final ModalidadTitulacionRepository modalidadRepo;
    private final PeriodoTitulacionRepository periodoRepo;

    public EleccionService(EleccionTitulacionRepository repo,
                           CarreraModalidadRepository carreraModalidadRepo,
                           EstudianteRepository estudianteRepo,
                           ModalidadTitulacionRepository modalidadRepo,
                           PeriodoTitulacionRepository periodoRepo) {
        this.repo = repo;
        this.carreraModalidadRepo = carreraModalidadRepo;
        this.estudianteRepo = estudianteRepo;
        this.modalidadRepo = modalidadRepo;
        this.periodoRepo = periodoRepo;
    }

    @Auditable(entidad = "EleccionTitulacion", accion = "SELECCIONAR_MODALIDAD")
    @Transactional
    public EleccionTitulacion elegir(Integer idEstudiante, Integer idModalidad, String motivo) {
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado: " + idEstudiante));
        if (estudiante.getCarrera() == null) {
            throw new IllegalStateException("El estudiante no tiene una carrera registrada");
        }
        Integer idCarrera = estudiante.getCarrera().getIdCarrera();

        boolean permitido = carreraModalidadRepo
                .existsById_IdCarreraAndId_IdModalidadAndActivoTrue(idCarrera, idModalidad);
        if (!permitido) {
            throw new IllegalArgumentException("La modalidad seleccionada no está habilitada para tu carrera");
        }

        Modalidadtitulacion modalidad = modalidadRepo.findById(idModalidad)
                .orElseThrow(() -> new EntityNotFoundException("Modalidad no encontrada: " + idModalidad));
        PeriodoTitulacion periodo = periodoRepo.findByActivoTrue()
                .orElseThrow(() -> new EntityNotFoundException("No hay un período de titulación activo"));

        EleccionTitulacion eleccion = repo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(idEstudiante, periodo.getIdPeriodo())
                .orElseGet(EleccionTitulacion::new);

        boolean esNueva = eleccion.getIdEleccion() == null;
        boolean cambiaModalidad = !esNueva && eleccion.getModalidad() != null
                && !eleccion.getModalidad().getIdModalidad().equals(idModalidad);

        eleccion.setEstudiante(estudiante);
        eleccion.setCarrera(estudiante.getCarrera());
        eleccion.setModalidad(modalidad);
        eleccion.setPeriodo(periodo);
        eleccion.setFechaEleccion(LocalDate.now());
        eleccion.setEstado(EstadoEleccion.ACTIVA);

        if (esNueva) {
            eleccion.setNumeroCambios(0);
        } else if (cambiaModalidad) {
            eleccion.setNumeroCambios((eleccion.getNumeroCambios() == null ? 0 : eleccion.getNumeroCambios()) + 1);
            eleccion.setMotivoUltimoCambio(motivo);
            eleccion.setFechaUltimoCambio(LocalDate.now());
        }

        return repo.save(eleccion);
    }
}
