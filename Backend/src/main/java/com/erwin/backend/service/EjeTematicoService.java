package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.EjeTematicoRequest;
import com.erwin.backend.dtos.EjeTematicoResponse;
import com.erwin.backend.dtos.PlanificacionEjeRequest;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.EjeTematico;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoEjeTematico;
import com.erwin.backend.enums.EstadoPlanificacionEje;
import com.erwin.backend.repository.CarreraRepository;
import com.erwin.backend.repository.DocenteCarreraRepository;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.EjeTematicoRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EjeTematicoService {

    private static final int MAX_EJES_ACTIVOS_POR_CARRERA_PERIODO = 3;

    private final EjeTematicoRepository ejeRepo;
    private final CarreraRepository carreraRepo;
    private final DocenteRepository docenteRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuthorizationService authorizationService;

    public EjeTematicoService(EjeTematicoRepository ejeRepo,
                               CarreraRepository carreraRepo,
                               DocenteRepository docenteRepo,
                               DocenteCarreraRepository docenteCarreraRepo,
                               PeriodoTitulacionRepository periodoRepo,
                               UsuarioRepository usuarioRepo,
                               AuthorizationService authorizationService) {
        this.ejeRepo = ejeRepo;
        this.carreraRepo = carreraRepo;
        this.docenteRepo = docenteRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
        this.periodoRepo = periodoRepo;
        this.usuarioRepo = usuarioRepo;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<EjeTematicoResponse> listarPorCarrera(Integer idCarrera, String periodo) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        List<EjeTematico> ejes = (periodo == null || periodo.isBlank())
                ? ejeRepo.findByCarrera_IdCarrera(idCarrera)
                : ejeRepo.findByCarrera_IdCarreraAndPeriodoAcademico(idCarrera, periodo);
        return ejes.stream().map(this::convertirADto).collect(Collectors.toList());
    }

    @Auditable(entidad = "EjeTematico", accion = "CREATE")
    public EjeTematicoResponse crear(EjeTematicoRequest req) {
        authorizationService.exigirPropiaCarreraSiCoordinador(req.getIdCarrera());
        if (req.getNombre() == null || req.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del eje temático es obligatorio");
        }
        if (req.getPeriodoAcademico() == null || req.getPeriodoAcademico().isBlank()) {
            throw new IllegalArgumentException("El periodo académico es obligatorio");
        }

        Carrera carrera = carreraRepo.findById(req.getIdCarrera())
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.getIdCarrera()));

        EstadoEjeTematico estado = parseEstado(req.getEstado(), EstadoEjeTematico.ACTIVO);

        if (estado == EstadoEjeTematico.ACTIVO) {
            validarLimiteActivos(req.getIdCarrera(), req.getPeriodoAcademico());
        }

        EjeTematico eje = new EjeTematico();
        eje.setCarrera(carrera);
        eje.setNombre(req.getNombre());
        eje.setDescripcion(req.getDescripcion());
        eje.setPeriodoAcademico(req.getPeriodoAcademico());
        eje.setEstado(estado);
        if (req.getIdDocenteResponsable() != null) {
            eje.setDocenteResponsable(getDocenteDelDistributivo(req.getIdDocenteResponsable(), req.getIdCarrera()));
        }

        return convertirADto(ejeRepo.save(eje));
    }

    @Auditable(entidad = "EjeTematico", accion = "UPDATE")
    public EjeTematicoResponse actualizar(Long id, EjeTematicoRequest req) {
        EjeTematico eje = ejeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + id));
        if (eje.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(eje.getCarrera().getIdCarrera());
        }
        if (req.getIdCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(req.getIdCarrera());
        }

        EstadoEjeTematico nuevoEstado = parseEstado(req.getEstado(), eje.getEstado());
        boolean seActiva = nuevoEstado == EstadoEjeTematico.ACTIVO && eje.getEstado() != EstadoEjeTematico.ACTIVO;
        if (seActiva) {
            validarLimiteActivos(req.getIdCarrera() != null ? req.getIdCarrera() : eje.getCarrera().getIdCarrera(),
                    req.getPeriodoAcademico() != null ? req.getPeriodoAcademico() : eje.getPeriodoAcademico());
        }

        if (req.getIdCarrera() != null) {
            eje.setCarrera(carreraRepo.findById(req.getIdCarrera())
                    .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.getIdCarrera())));
        }
        if (req.getNombre() != null) eje.setNombre(req.getNombre());
        if (req.getDescripcion() != null) eje.setDescripcion(req.getDescripcion());
        if (req.getPeriodoAcademico() != null) eje.setPeriodoAcademico(req.getPeriodoAcademico());
        if (req.getIdDocenteResponsable() != null) {
            Integer idCarreraEfectiva = req.getIdCarrera() != null ? req.getIdCarrera() : eje.getCarrera().getIdCarrera();
            eje.setDocenteResponsable(getDocenteDelDistributivo(req.getIdDocenteResponsable(), idCarreraEfectiva));
        }
        eje.setEstado(nuevoEstado);

        return convertirADto(ejeRepo.save(eje));
    }

    // ═══════════════════════════════════════════════════════════════
    // Planificación de ejes temáticos (TAREA 1.1)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "EjeTematico", accion = "PLANIFICAR")
    public EjeTematicoResponse planificar(Long id, PlanificacionEjeRequest req) {
        EjeTematico eje = ejeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + id));
        if (eje.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(eje.getCarrera().getIdCarrera());
        }

        if (req.getCronograma() == null || req.getCronograma().isBlank()) {
            throw new IllegalArgumentException("El cronograma es obligatorio");
        }
        if (req.getContenidosTematicos() == null || req.getContenidosTematicos().isBlank()) {
            throw new IllegalArgumentException("Los contenidos temáticos son obligatorios");
        }

        PeriodoTitulacion periodo = periodoRepo.findByDescripcion(eje.getPeriodoAcademico()).orElse(null);
        if (periodo != null && periodo.getFechaInicio() != null
                && !LocalDate.now().isBefore(periodo.getFechaInicio())) {
            throw new IllegalStateException(
                    "La planificación debe presentarse antes de la primera semana del periodo académico "
                            + "(inicio del periodo: " + periodo.getFechaInicio() + ")");
        }

        eje.setCronograma(req.getCronograma());
        eje.setContenidosTematicos(req.getContenidosTematicos());
        eje.setEstadoPlanificacion(EstadoPlanificacionEje.BORRADOR);
        eje.setFechaPlanificacion(LocalDateTime.now());

        return convertirADto(ejeRepo.save(eje));
    }

    @Auditable(entidad = "EjeTematico", accion = "APROBAR_PLANIFICACION")
    public EjeTematicoResponse aprobarPlanificacion(Long id, Integer idUsuarioAprobador) {
        EjeTematico eje = ejeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + id));
        if (eje.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(eje.getCarrera().getIdCarrera());
        }

        if (eje.getEstadoPlanificacion() != EstadoPlanificacionEje.BORRADOR) {
            throw new IllegalStateException(
                    "Solo se puede aprobar una planificación en estado BORRADOR (actual: "
                            + eje.getEstadoPlanificacion() + ")");
        }

        Usuario aprobador = usuarioRepo.findById(idUsuarioAprobador)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuarioAprobador));

        eje.setEstadoPlanificacion(EstadoPlanificacionEje.APROBADO_COMITE_ACADEMICO);
        eje.setFechaAprobacionPlanificacion(LocalDateTime.now());
        eje.setAprobadoPorPlanificacion(aprobador);

        return convertirADto(ejeRepo.save(eje));
    }

    // ═══════════════════════════════════════════════════════════════
    // Ejes con docente responsable asignado (TAREA 1.2)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<EjeTematicoResponse> listarConDocentes(Integer idCarrera) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        return ejeRepo.findByCarrera_IdCarreraAndDocenteResponsableIsNotNull(idCarrera)
                .stream().map(this::convertirADto).collect(Collectors.toList());
    }

    @Auditable(entidad = "EjeTematico", accion = "DELETE")
    public void eliminar(Long id) {
        EjeTematico eje = ejeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + id));
        if (eje.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(eje.getCarrera().getIdCarrera());
        }
        ejeRepo.deleteById(id);
    }

    private void validarLimiteActivos(Integer idCarrera, String periodo) {
        long activos = ejeRepo.countByCarrera_IdCarreraAndPeriodoAcademicoAndEstado(
                idCarrera, periodo, EstadoEjeTematico.ACTIVO);
        if (activos >= MAX_EJES_ACTIVOS_POR_CARRERA_PERIODO) {
            throw new IllegalArgumentException(
                    "La carrera ya tiene " + MAX_EJES_ACTIVOS_POR_CARRERA_PERIODO
                            + " ejes temáticos activos en el periodo " + periodo);
        }
    }

    private Docente getDocenteDelDistributivo(Integer idDocente, Integer idCarrera) {
        Docente docente = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + idDocente));
        if (idCarrera != null && !docenteCarreraRepo
                .existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(idDocente, idCarrera)) {
            throw new IllegalStateException(
                    "El docente " + idDocente + " no pertenece al distributivo de la carrera " + idCarrera);
        }
        return docente;
    }

    private EstadoEjeTematico parseEstado(String estado, EstadoEjeTematico porDefecto) {
        if (estado == null || estado.isBlank()) return porDefecto;
        try {
            return EstadoEjeTematico.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de eje temático inválido: " + estado);
        }
    }

    private EjeTematicoResponse convertirADto(EjeTematico e) {
        return new EjeTematicoResponse(
                e.getId(),
                e.getCarrera() != null ? e.getCarrera().getIdCarrera() : null,
                e.getCarrera() != null ? e.getCarrera().getNombre() : null,
                e.getNombre(),
                e.getDescripcion(),
                e.getDocenteResponsable() != null ? e.getDocenteResponsable().getIdDocente() : null,
                e.getDocenteResponsable() != null ? fullName(e.getDocenteResponsable()) : null,
                e.getPeriodoAcademico(),
                e.getEstado() != null ? e.getEstado().name() : null,
                e.getCronograma(),
                e.getContenidosTematicos(),
                e.getEstadoPlanificacion() != null ? e.getEstadoPlanificacion().name() : null,
                e.getFechaPlanificacion(),
                e.getFechaAprobacionPlanificacion(),
                e.getAprobadoPorPlanificacion() != null
                        ? (n(e.getAprobadoPorPlanificacion().getNombres()) + " " + n(e.getAprobadoPorPlanificacion().getApellidos())).trim()
                        : null
        );
    }

    private String n(String s) { return s == null ? "" : s; }

    private String fullName(Docente docente) {
        if (docente.getUsuario() == null) return null;
        String nombres = docente.getUsuario().getNombres() != null ? docente.getUsuario().getNombres() : "";
        String apellidos = docente.getUsuario().getApellidos() != null ? docente.getUsuario().getApellidos() : "";
        return (nombres + " " + apellidos).trim();
    }
}
