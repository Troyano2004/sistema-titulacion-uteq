package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.CambiarOpcionMatriculaRequest;
import com.erwin.backend.dtos.MatriculaTitulacionRequest;
import com.erwin.backend.dtos.MatriculaTitulacionResponse;
import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.MatriculaTitulacion;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.entities.PensumImportado;
import com.erwin.backend.entities.RequisitoImportado;
import com.erwin.backend.entities.Tipotrabajotitulacion;
import com.erwin.backend.enums.EstadoAsignaturaImportada;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.MatriculaTitulacionRepository;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.PensumImportadoRepository;
import com.erwin.backend.repository.RequisitoImportadoRepository;
import com.erwin.backend.repository.TipoTrabajoTitulacionRepository;
import com.erwin.backend.util.NivelAcademicoUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatriculaTitulacionService {

    /** Debe coincidir con el nombre real usado en todo el sistema (ver ComisionTemasController). */
    private static final String NOMBRE_MODALIDAD_COMPLEXIVO = "Examen Complexivo";
    private static final int MAX_CAMBIOS_OPCION = 1;
    private static final int SEMANA_LIMITE_CAMBIO_OPCION = 4;

    private final MatriculaTitulacionRepository matriculaRepo;
    private final EstudianteRepository estudianteRepo;
    private final ModalidadTitulacionRepository modalidadRepo;
    private final TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    private final CoordinadorRepository coordinadorRepo;
    private final EmailService emailService;
    private final PensumImportadoRepository pensumImportadoRepo;
    private final RequisitoImportadoRepository requisitoImportadoRepo;

    public MatriculaTitulacionService(MatriculaTitulacionRepository matriculaRepo,
                                       EstudianteRepository estudianteRepo,
                                       ModalidadTitulacionRepository modalidadRepo,
                                       TipoTrabajoTitulacionRepository tipoTrabajoRepo,
                                       CoordinadorRepository coordinadorRepo,
                                       EmailService emailService,
                                       PensumImportadoRepository pensumImportadoRepo,
                                       RequisitoImportadoRepository requisitoImportadoRepo) {
        this.matriculaRepo = matriculaRepo;
        this.estudianteRepo = estudianteRepo;
        this.modalidadRepo = modalidadRepo;
        this.tipoTrabajoRepo = tipoTrabajoRepo;
        this.coordinadorRepo = coordinadorRepo;
        this.emailService = emailService;
        this.pensumImportadoRepo = pensumImportadoRepo;
        this.requisitoImportadoRepo = requisitoImportadoRepo;
    }

    @Transactional(readOnly = true)
    public List<MatriculaTitulacionResponse> listarPorEstudiante(Integer idEstudiante) {
        return matriculaRepo.findByEstudiante_IdEstudiante(idEstudiante).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "MatriculaTitulacion", accion = "CREATE")
    public MatriculaTitulacionResponse crear(MatriculaTitulacionRequest req) {
        if (req.getPeriodoAcademico() == null || req.getPeriodoAcademico().isBlank()) {
            throw new IllegalArgumentException("El periodo académico es obligatorio");
        }
        NivelMatricula nivel = parseNivel(req.getNivel());

        Estudiante estudiante = estudianteRepo.findById(req.getIdEstudiante())
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado: " + req.getIdEstudiante()));

        if (nivel == NivelMatricula.TITULACION_I) {
            validarPrerrequisitosTitulacionI(estudiante);
        } else {
            validarPrerrequisitosTitulacionII(estudiante, req);
        }

        if (req.getIdOpcionTitulacion() == null) {
            throw new IllegalArgumentException("Debe seleccionar una opción de titulación");
        }
        Modalidadtitulacion opcion = modalidadRepo.findById(req.getIdOpcionTitulacion())
                .orElseThrow(() -> new EntityNotFoundException("Opción de titulación no encontrada: " + req.getIdOpcionTitulacion()));

        Tipotrabajotitulacion subtipo = validarYResolverSubtipo(opcion, req.getIdSubtipoTrabajo());

        MatriculaTitulacion matricula = new MatriculaTitulacion();
        matricula.setEstudiante(estudiante);
        matricula.setPeriodoAcademico(req.getPeriodoAcademico());
        matricula.setNivel(nivel);
        matricula.setOpcionTitulacion(opcion);
        matricula.setSubtipoTrabajo(subtipo);
        matricula.setFechaMatricula(LocalDateTime.now());
        matricula.setEstado(EstadoMatricula.ACTIVA);
        matricula.setNumeroCambiosOpcion(0);

        return convertirADto(matriculaRepo.save(matricula));
    }

    @Auditable(entidad = "MatriculaTitulacion", accion = "CAMBIAR_OPCION", capturarArgs = true)
    public MatriculaTitulacionResponse cambiarOpcion(Long id, CambiarOpcionMatriculaRequest req) {
        MatriculaTitulacion matricula = matriculaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Matrícula no encontrada: " + id));

        if (matricula.getNivel() != NivelMatricula.TITULACION_I) {
            throw new IllegalStateException("No se puede cambiar la opción de titulación en Titulación II");
        }
        if (matricula.getEstado() != EstadoMatricula.ACTIVA) {
            throw new IllegalStateException("Solo se puede cambiar la opción de una matrícula ACTIVA");
        }
        if (matricula.getNumeroCambiosOpcion() != null && matricula.getNumeroCambiosOpcion() >= MAX_CAMBIOS_OPCION) {
            throw new IllegalStateException(
                    "El estudiante ya agotó el máximo de " + MAX_CAMBIOS_OPCION + " cambio(s) de opción de titulación");
        }
        if (req.getSemanaActual() == null) {
            throw new IllegalArgumentException("Debe indicar la semana académica actual (semanaActual)");
        }
        if (req.getIdOpcionTitulacion() == null) {
            throw new IllegalArgumentException("Debe indicar la nueva opción de titulación");
        }

        boolean fueraDePlazo = req.getSemanaActual() > SEMANA_LIMITE_CAMBIO_OPCION;
        if (fueraDePlazo) {
            if (req.getMotivoMedico() == null || req.getMotivoMedico().isBlank() || req.getDocumentoAdjuntoId() == null) {
                throw new IllegalStateException(
                        "El cambio de opción solo está permitido hasta la semana " + SEMANA_LIMITE_CAMBIO_OPCION
                                + " del periodo; fuera de ese plazo se requiere motivoMedico y el documento de soporte (documentoAdjuntoId)");
            }
        }

        Modalidadtitulacion opcion = modalidadRepo.findById(req.getIdOpcionTitulacion())
                .orElseThrow(() -> new EntityNotFoundException("Opción de titulación no encontrada: " + req.getIdOpcionTitulacion()));
        Tipotrabajotitulacion subtipo = validarYResolverSubtipo(opcion, req.getIdSubtipoTrabajo());

        matricula.setOpcionTitulacion(opcion);
        matricula.setSubtipoTrabajo(subtipo);
        matricula.setNumeroCambiosOpcion(matricula.getNumeroCambiosOpcion() + 1);
        matricula.setMotivoUltimoCambio(fueraDePlazo ? ("CASO_MEDICO: " + req.getMotivoMedico()) : req.getMotivo());
        matricula.setFechaUltimoCambio(LocalDateTime.now());
        if (fueraDePlazo) {
            matricula.setDocumentoAdjuntoIdCasoMedico(req.getDocumentoAdjuntoId());
        }

        MatriculaTitulacion guardada = matriculaRepo.save(matricula);
        notificarCambioOpcionACoordinador(guardada);

        return convertirADto(guardada);
    }

    private void validarPrerrequisitosTitulacionI(Estudiante estudiante) {
        int penultimo = NivelAcademicoUtil.penultimo(estudiante.getCarrera());
        if (estudiante.getNivelSemestre() == null || estudiante.getNivelSemestre() != penultimo) {
            throw new IllegalStateException(
                    "El estudiante debe estar en el penúltimo nivel (" + penultimo
                            + ") de su carrera para matricularse en Titulación I");
        }

        boolean yaTieneT1 = matriculaRepo.existsByEstudiante_IdEstudianteAndNivelAndEstadoIn(
                estudiante.getIdEstudiante(), NivelMatricula.TITULACION_I,
                EnumSet.of(EstadoMatricula.ACTIVA, EstadoMatricula.COMPLETADA));
        if (yaTieneT1) {
            throw new IllegalStateException(
                    "El estudiante ya cuenta con una matrícula de Titulación I activa o completada");
        }

        // Módulo 20 (TAREA 2.3): si hay pensum importado del SGA, se verifica
        // contra esos datos reales en vez de confiar ciegamente en el nivel;
        // si no hay datos importados, se sigue aceptando solo la verificación
        // por nivel de arriba (flujo manual, sin bloquear).
        validarPensumSiHayDatosImportados(estudiante.getIdEstudiante());
    }

    private void validarPensumSiHayDatosImportados(Integer idEstudiante) {
        List<PensumImportado> pensum = pensumImportadoRepo.findByEstudiante_IdEstudiante(idEstudiante);
        if (pensum.isEmpty()) return; // sin datos importados -> no se bloquea (REGLAS)

        Set<String> aprobadas = pensum.stream()
                .filter(p -> p.getEstado() == EstadoAsignaturaImportada.APROBADA)
                .map(PensumImportado::getCodigoAsignatura)
                .collect(Collectors.toSet());
        List<String> pendientes = pensum.stream()
                .filter(p -> p.getEstado() == EstadoAsignaturaImportada.REPROBADA)
                .map(PensumImportado::getCodigoAsignatura)
                .filter(codigo -> !aprobadas.contains(codigo))
                .distinct()
                .toList();

        if (!pendientes.isEmpty()) {
            throw new IllegalStateException(
                    "Según el pensum importado del SGA, el estudiante tiene asignaturas reprobadas sin aprobar: "
                            + String.join(", ", pendientes));
        }
    }

    private void validarPrerrequisitosTitulacionII(Estudiante estudiante, MatriculaTitulacionRequest req) {
        boolean tituacionICompletada = matriculaRepo
                .findByEstudiante_IdEstudianteAndNivelAndEstado(
                        estudiante.getIdEstudiante(), NivelMatricula.TITULACION_I, EstadoMatricula.COMPLETADA)
                .isPresent();
        if (!tituacionICompletada) {
            throw new IllegalStateException("El estudiante debe tener Titulación I con estado COMPLETADA para matricularse en Titulación II");
        }

        // Módulo 20 (TAREA 2.3): si el SGA ya importó los requisitos de este
        // estudiante, esos datos son la fuente de verdad; si no existen, se
        // sigue aceptando el flujo manual con los booleanos del request tal
        // como funcionaba antes (REGLAS).
        Optional<RequisitoImportado> importado = requisitoImportadoRepo.findByEstudiante_IdEstudiante(estudiante.getIdEstudiante());

        boolean idioma = importado.map(RequisitoImportado::getIdiomaAprobado)
                .orElse(Boolean.TRUE.equals(req.getIdiomaExtranjeroAprobado()));
        boolean practicas = importado.map(RequisitoImportado::getPracticasLaborales)
                .orElse(Boolean.TRUE.equals(req.getPracticasLaboralesAprobadas()));
        boolean servicio = importado.map(RequisitoImportado::getServicioComunitario)
                .orElse(Boolean.TRUE.equals(req.getServicioComunitarioAprobado()));

        List<String> faltantes = new ArrayList<>();
        if (!idioma) faltantes.add("idioma extranjero");
        if (!practicas) faltantes.add("prácticas laborales");
        if (!servicio) faltantes.add("servicio comunitario");

        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "No cumple los requisitos del Art. 18 para matricularse en Titulación II: falta aprobar "
                            + String.join(", ", faltantes));
        }
    }

    /** TIC = cualquier opción de titulación distinta de Examen Complexivo. Exige subtipo; Complexivo lo prohíbe. */
    private Tipotrabajotitulacion validarYResolverSubtipo(Modalidadtitulacion opcion, Integer idSubtipo) {
        boolean esComplexivo = NOMBRE_MODALIDAD_COMPLEXIVO.equalsIgnoreCase(opcion.getNombre());

        if (esComplexivo) {
            if (idSubtipo != null) {
                throw new IllegalArgumentException("El subtipo de trabajo no aplica para la opción " + NOMBRE_MODALIDAD_COMPLEXIVO);
            }
            return null;
        }

        if (idSubtipo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el subtipo de trabajo (Proyecto de Investigación, Artículo Científico, "
                            + "Estudio de Caso o Proyecto Tecnológico) al elegir Trabajo de Integración Curricular");
        }

        Tipotrabajotitulacion subtipo = tipoTrabajoRepo.findById(idSubtipo)
                .orElseThrow(() -> new EntityNotFoundException("Subtipo de trabajo no encontrado: " + idSubtipo));

        if (subtipo.getModalidadTitulacion() == null
                || !subtipo.getModalidadTitulacion().getIdModalidad().equals(opcion.getIdModalidad())) {
            throw new IllegalArgumentException("El subtipo de trabajo no pertenece a la opción de titulación indicada");
        }
        return subtipo;
    }

    private void notificarCambioOpcionACoordinador(MatriculaTitulacion matricula) {
        Estudiante estudiante = matricula.getEstudiante();
        if (estudiante.getCarrera() == null) return;

        try {
            Coordinador coordinador = coordinadorRepo
                    .findFirstByCarrera_IdCarreraAndActivoTrue(estudiante.getCarrera().getIdCarrera())
                    .orElse(null);
            if (coordinador == null || coordinador.getUsuario() == null) return;

            String correoCoordinador = coordinador.getUsuario().getCorreoInstitucional();
            if (correoCoordinador == null || correoCoordinador.isBlank()) return;

            String nombreEstudiante = estudiante.getUsuario() != null
                    ? (estudiante.getUsuario().getNombres() + " " + estudiante.getUsuario().getApellidos()).trim()
                    : ("Estudiante #" + estudiante.getIdEstudiante());

            emailService.notificarCambioOpcionTitulacion(
                    correoCoordinador,
                    nombreEstudiante,
                    matricula.getOpcionTitulacion() != null ? matricula.getOpcionTitulacion().getNombre() : "",
                    matricula.getMotivoUltimoCambio());
        } catch (Exception ignored) {
            // best-effort: la falla de notificación no debe revertir el cambio ya auditado
        }
    }

    private NivelMatricula parseNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            throw new IllegalArgumentException("El nivel de matrícula es obligatorio");
        }
        try {
            return NivelMatricula.valueOf(nivel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nivel de matrícula inválido: " + nivel);
        }
    }

    private MatriculaTitulacionResponse convertirADto(MatriculaTitulacion m) {
        return new MatriculaTitulacionResponse(
                m.getId(),
                m.getEstudiante() != null ? m.getEstudiante().getIdEstudiante() : null,
                m.getEstudiante() != null && m.getEstudiante().getUsuario() != null
                        ? (m.getEstudiante().getUsuario().getNombres() + " " + m.getEstudiante().getUsuario().getApellidos()).trim()
                        : null,
                m.getPeriodoAcademico(),
                m.getNivel() != null ? m.getNivel().name() : null,
                m.getOpcionTitulacion() != null ? m.getOpcionTitulacion().getIdModalidad() : null,
                m.getOpcionTitulacion() != null ? m.getOpcionTitulacion().getNombre() : null,
                m.getSubtipoTrabajo() != null ? m.getSubtipoTrabajo().getIdTipoTrabajo() : null,
                m.getSubtipoTrabajo() != null ? m.getSubtipoTrabajo().getNombre() : null,
                m.getFechaMatricula(),
                m.getEstado() != null ? m.getEstado().name() : null,
                m.getNumeroCambiosOpcion(),
                m.getMotivoUltimoCambio(),
                m.getFechaUltimoCambio(),
                m.getDocumentoAdjuntoIdCasoMedico()
        );
    }
}
