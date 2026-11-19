package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoAnteproyecto;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.*;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class Dt2Service {

    private static final int MIN_ASESORIAS_CORTE = 5;
    private static final int MIN_REUNIONES_MENSUALES = 2;
    private static final int DIAS_GRACIA_CIERRE_CORTE = 5;
    private static final int MIN_MIEMBROS_TRIBUNAL = 3;
    private static final BigDecimal UMBRAL_ANTIPLAGIO = new BigDecimal("10.00");
    private static final BigDecimal NOTA_APROBATORIA_SUSTENTACION = new BigDecimal("7.00");
    private static final BigDecimal NOTA_APROBATORIA_PREDEFENSA = new BigDecimal("7.00");
    private static final int DIAS_SEGUNDA_OPORTUNIDAD = 15;

    private final ProyectoTitulacionRepository proyectoRepo;
    private final AnteproyectoTitulacionRepository anteproyectoRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final DocenteRepository docenteRepo;
    private final UsuarioRepository usuarioRepo;
    private final Dt2AsignacionRepository dt2AsignacionRepo;
    private final BitacoraAsignacionRepository bitacoraRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final AsesoriaDirectorRepository asesoriaRepo;
    private final ActaCorteRepository actaCorteRepo;
    private final AntiplacioIntentoRepository antiplacioRepo;
    private final SustentacionRepository sustentacionRepo;
    private final EvaluacionSustentacionRepository evaluacionRepo;
    private final DocumentoPrevioSustentacionRepository docPrevioRepo;
    private final ActaGradoRepository actaGradoRepo;
    private final DocumentStorageService storageService;
    private final EmailService emailService;
    private final OportunidadService oportunidadService;

    public Dt2Service(ProyectoTitulacionRepository proyectoRepo,
                      AnteproyectoTitulacionRepository anteproyectoRepo,
                      DocumentoTitulacionRepository documentoRepo,
                      DocenteRepository docenteRepo,
                      UsuarioRepository usuarioRepo,
                      Dt2AsignacionRepository dt2AsignacionRepo,
                      BitacoraAsignacionRepository bitacoraRepo,
                      TribunalProyectoRepository tribunalRepo,
                      AsesoriaDirectorRepository asesoriaRepo,
                      ActaCorteRepository actaCorteRepo,
                      AntiplacioIntentoRepository antiplacioRepo,
                      SustentacionRepository sustentacionRepo,
                      EvaluacionSustentacionRepository evaluacionRepo,
                      DocumentoPrevioSustentacionRepository docPrevioRepo,
                      ActaGradoRepository actaGradoRepo,
                      DocumentStorageService storageService,
                      EmailService emailService,
                      OportunidadService oportunidadService) {
        this.proyectoRepo      = proyectoRepo;
        this.anteproyectoRepo  = anteproyectoRepo;
        this.documentoRepo     = documentoRepo;
        this.docenteRepo       = docenteRepo;
        this.usuarioRepo       = usuarioRepo;
        this.dt2AsignacionRepo = dt2AsignacionRepo;
        this.bitacoraRepo      = bitacoraRepo;
        this.tribunalRepo      = tribunalRepo;
        this.asesoriaRepo      = asesoriaRepo;
        this.actaCorteRepo     = actaCorteRepo;
        this.antiplacioRepo    = antiplacioRepo;
        this.sustentacionRepo  = sustentacionRepo;
        this.evaluacionRepo    = evaluacionRepo;
        this.docPrevioRepo     = docPrevioRepo;
        this.actaGradoRepo     = actaGradoRepo;
        this.storageService    = storageService;
        this.emailService      = emailService;
        this.oportunidadService = oportunidadService;
    }

    // =========================================================
    // MÓDULO 1 — Configuración inicial de Titulación II
    // =========================================================

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosPendientesConfiguracion() {
        return proyectoRepo.findAll().stream()
                .filter(p -> "ANTEPROYECTO".equalsIgnoreCase(p.getEstado())
                        || "BORRADOR".equalsIgnoreCase(p.getEstado())
                        || "DESARROLLO".equalsIgnoreCase(p.getEstado()))
                .filter(p -> {
                    AnteproyectoTitulacion ante = anteproyectoRepo
                            .findByPropuesta_IdPropuesta(p.getPropuesta().getIdPropuesta())
                            .orElse(null);
                    return (ante != null && ante.getEstado() == EstadoAnteproyecto.APROBADO)
                            || "DESARROLLO".equalsIgnoreCase(p.getEstado());
                })
                .filter(p -> {
                    boolean tieneDt2      = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(p.getIdProyecto()).isPresent();
                    boolean tieneDirector = p.getDirector() != null;
                    boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(p.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;
                    return !(tieneDt2 && tieneDirector && tieneTribunal);
                })
                .map(this::toProyectoDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarTodosProyectosConfiguracion() {
        return proyectoRepo.findAll().stream()
                .filter(p -> "ANTEPROYECTO".equalsIgnoreCase(p.getEstado())
                        || "BORRADOR".equalsIgnoreCase(p.getEstado())
                        || "DESARROLLO".equalsIgnoreCase(p.getEstado())
                        || "PREDEFENSA".equalsIgnoreCase(p.getEstado())
                        || "SUSTENTACION".equalsIgnoreCase(p.getEstado())
                        || "FINALIZADO".equalsIgnoreCase(p.getEstado()))
                .filter(p -> {
                    AnteproyectoTitulacion ante = anteproyectoRepo
                            .findByPropuesta_IdPropuesta(p.getPropuesta().getIdPropuesta())
                            .orElse(null);
                    return (ante != null && ante.getEstado() == EstadoAnteproyecto.APROBADO)
                            || "DESARROLLO".equalsIgnoreCase(p.getEstado())
                            || "PREDEFENSA".equalsIgnoreCase(p.getEstado())
                            || "SUSTENTACION".equalsIgnoreCase(p.getEstado())
                            || "FINALIZADO".equalsIgnoreCase(p.getEstado());
                })
                .map(this::toProyectoDto)
                .sorted((a, b) -> {
                    boolean ac = Boolean.TRUE.equals(a.getConfiguracionCompleta());
                    boolean bc = Boolean.TRUE.equals(b.getConfiguracionCompleta());
                    if (ac == bc) return 0;
                    return ac ? 1 : -1;
                })
                .collect(Collectors.toList());
    }

    private Dt2Dtos.ProyectoPendienteConfiguracionDto toProyectoDto(ProyectoTitulacion p) {
        boolean tieneDt2      = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(p.getIdProyecto()).isPresent();
        boolean tieneDirector = p.getDirector() != null;
        boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(p.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;

        Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
        dto.setIdProyecto(p.getIdProyecto());
        dto.setTitulo(p.getTitulo());
        dto.setIdEstudiante(p.getPropuesta().getEstudiante().getIdEstudiante());
        dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
        dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
        dto.setPeriodo(p.getPeriodo().getDescripcion());
        dto.setEstadoProyecto(p.getEstado());
        dto.setTieneDocenteDt2(tieneDt2);
        dto.setTieneDirector(tieneDirector);
        dto.setTieneTribunal(tieneTribunal);
        dto.setConfiguracionCompleta(tieneDt2 && tieneDirector && tieneTribunal);
        return dto;
    }

    @Auditable(entidad = "Dt2Asignacion", accion = "CREATE")
    @Transactional
    public Dt2Dtos.MensajeDto asignarDocenteDt2(Dt2Dtos.AsignarDocenteDt2Request req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        Docente docente      = getDocente(req.getIdDocenteDt2());
        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());

        dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(req.getIdProyecto())
                .ifPresent(a -> { a.setActivo(false); dt2AsignacionRepo.save(a); });

        Dt2Asignacion asignacion = new Dt2Asignacion();
        asignacion.setProyecto(proyecto);
        asignacion.setDocenteDt2(docente);
        asignacion.setAsignadoPor(realizadoPor);
        asignacion.setPeriodo(req.getPeriodo());
        asignacion.setObservacion(req.getObservacion());
        asignacion.setActivo(true);
        dt2AsignacionRepo.save(asignacion);

        registrarBitacora(proyecto, "DOCENTE_DT2", docente.getIdDocente(),
                fullName(docente.getUsuario()), null, realizadoPor,
                req.getPeriodo(), req.getObservacion());

        intentarTransicionDesarrollo(proyecto);

        String emailDocente = docente.getUsuario() != null ? docente.getUsuario().getCorreoInstitucional() : null;
        if (emailDocente != null && !emailDocente.isBlank()) {
            String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
            String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
            emailService.notificarAsignacionDocenteDt2(emailDocente, fullName(docente.getUsuario()),
                    proyecto.getTitulo(), estudiante, periodo);
        }

        return new Dt2Dtos.MensajeDto("Docente DT2 asignado correctamente", proyecto.getEstado(), true);
    }

    @Auditable(entidad = "ProyectoTitulacion", accion = "CAMBIAR_DIRECTOR")
    @Transactional
    public Dt2Dtos.MensajeDto asignarDirector(Dt2Dtos.AsignarDirectorRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        boolean esCambio = proyecto.getDirector() != null;
        if (esCambio && (req.getMotivo() == null || req.getMotivo().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cambiar el Director debe ingresar un motivo justificado");
        }
        // Se captura ANTES de sobreescribir — la bitácora debe registrar quién era el director
        // anterior, quién es el nuevo, quién autorizó el cambio y cuándo (Módulo de auditoría).
        String directorAnteriorNombre = esCambio ? fullName(proyecto.getDirector().getUsuario()) : null;

        Docente director     = getDocente(req.getIdDirector());
        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());

        proyecto.setDirector(director);
        proyectoRepo.save(proyecto);

        documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())
                .ifPresent(doc -> { doc.setDirector(director); documentoRepo.save(doc); });

        String observacion = esCambio
                ? "Director anterior: " + directorAnteriorNombre + ". Motivo: " + req.getMotivo()
                : req.getMotivo();
        registrarBitacora(proyecto, "DIRECTOR", director.getIdDocente(),
                fullName(director.getUsuario()), null, realizadoPor,
                req.getPeriodo(), observacion);

        intentarTransicionDesarrollo(proyecto);

        String emailDirector = director.getUsuario() != null ? director.getUsuario().getCorreoInstitucional() : null;
        if (emailDirector != null && !emailDirector.isBlank()) {
            String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
            String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
            emailService.notificarAsignacionDirector(emailDirector, fullName(director.getUsuario()),
                    proyecto.getTitulo(), estudiante, periodo);
        }

        return new Dt2Dtos.MensajeDto("Director asignado correctamente", proyecto.getEstado(), true);
    }

    @Auditable(entidad = "TribunalProyecto", accion = "ASIGNAR_O_REEMPLAZAR")
    @Transactional
    public Dt2Dtos.MensajeDto asignarTribunal(Dt2Dtos.AsignarTribunalDt2Request req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "ANTEPROYECTO", "DESARROLLO");
        validarAnteproyectoAprobado(proyecto);

        if (req.getMiembros() == null || req.getMiembros().size() < MIN_MIEMBROS_TRIBUNAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren al menos " + MIN_MIEMBROS_TRIBUNAL + " miembros del tribunal");
        }
        validarComposicionTribunal(req.getMiembros());

        Usuario realizadoPor = getUsuario(req.getIdRealizadoPor());

        // Se captura ANTES de borrar — la bitácora debe poder mostrar a quién
        // reemplaza cada nuevo miembro del tribunal (Módulo de auditoría).
        List<TribunalProyecto> miembrosAnteriores = tribunalRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto());
        String reemplazaA = miembrosAnteriores.isEmpty() ? null
                : "Reemplaza a: " + miembrosAnteriores.stream()
                        .map(t -> fullName(t.getDocente().getUsuario()) + " (" + t.getCargo() + ")")
                        .collect(Collectors.joining(", "));

        tribunalRepo.deleteByProyecto_IdProyecto(proyecto.getIdProyecto());

        for (Dt2Dtos.MiembroTribunalRequest m : req.getMiembros()) {
            Docente docente = getDocente(m.getIdDocente());
            TribunalProyecto tp = new TribunalProyecto();
            tp.setProyecto(proyecto);
            tp.setDocente(docente);
            tp.setCargo(m.getCargo() != null ? m.getCargo().toUpperCase() : "VOCAL");
            tribunalRepo.save(tp);

            registrarBitacora(proyecto, "TRIBUNAL", docente.getIdDocente(),
                    fullName(docente.getUsuario()), tp.getCargo(), realizadoPor,
                    req.getPeriodo(), reemplazaA);

            String emailTribunal = docente.getUsuario() != null ? docente.getUsuario().getCorreoInstitucional() : null;
            if (emailTribunal != null && !emailTribunal.isBlank()) {
                String estudiante = fullName(proyecto.getPropuesta().getEstudiante().getUsuario());
                String periodo    = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : req.getPeriodo();
                emailService.notificarAsignacionTribunal(emailTribunal, fullName(docente.getUsuario()),
                        tp.getCargo(), proyecto.getTitulo(), estudiante, periodo);
            }
        }

        intentarTransicionDesarrollo(proyecto);
        return new Dt2Dtos.MensajeDto("Tribunal registrado correctamente", proyecto.getEstado(), true);
    }

    /**
     * Reglamento UIC-UTEQ: el tribunal de un proyecto debe componerse de
     * exactamente 1 PRESIDENTE y 2 VOCAL. No se permite guardar una
     * composición distinta.
     */
    private void validarComposicionTribunal(List<Dt2Dtos.MiembroTribunalRequest> miembros) {
        long presidentes = miembros.stream()
                .filter(m -> "PRESIDENTE".equalsIgnoreCase(m.getCargo()))
                .count();
        long vocales = miembros.stream()
                .filter(m -> "VOCAL".equalsIgnoreCase(m.getCargo()))
                .count();

        if (presidentes != 1 || vocales != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tribunal debe estar compuesto exactamente por 1 PRESIDENTE y 2 VOCAL");
        }
    }

    @Transactional
    public Dt2Dtos.ConfiguracionProyectoDto getConfiguracion(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        Dt2Dtos.ConfiguracionProyectoDto dto = new Dt2Dtos.ConfiguracionProyectoDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstadoProyecto(proyecto.getEstado());

        dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(idProyecto).ifPresent(a -> {
            dto.setDocenteDt2(fullName(a.getDocenteDt2().getUsuario()));
            dto.setIdDocenteDt2(a.getDocenteDt2().getIdDocente());
        });

        DocumentoTitulacion doc      = documentoRepo.findByProyecto_IdProyecto(idProyecto).orElse(null);
        Docente             director = doc != null && doc.getDirector() != null ? doc.getDirector() : proyecto.getDirector();
        if (director != null) {
            dto.setDirector(fullName(director.getUsuario()));
            dto.setIdDirector(director.getIdDocente());
        }

        List<Dt2Dtos.MiembroTribunalDto> miembros = tribunalRepo.findByProyecto_IdProyecto(idProyecto)
                .stream().map(tp -> {
                    Dt2Dtos.MiembroTribunalDto m = new Dt2Dtos.MiembroTribunalDto();
                    m.setIdTribunal(tp.getIdTribunal());
                    m.setIdDocente(tp.getDocente().getIdDocente());
                    m.setNombre(fullName(tp.getDocente().getUsuario()));
                    m.setCargo(tp.getCargo());
                    return m;
                }).collect(Collectors.toList());
        dto.setTribunal(miembros);

        boolean completa = dto.getDocenteDt2() != null
                && dto.getDirector() != null
                && miembros.size() >= MIN_MIEMBROS_TRIBUNAL;
        dto.setConfiguracionCompleta(completa);

        List<Dt2Dtos.BitacoraDto> bitacora = bitacoraRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().map(b -> {
                    Dt2Dtos.BitacoraDto bd = new Dt2Dtos.BitacoraDto();
                    bd.setTipo(b.getTipoAsignacion());
                    bd.setNombreAsignado(b.getNombreAsignado());
                    bd.setCargo(b.getCargo());
                    bd.setRealizadoPor(fullName(b.getRealizadoPor()));
                    bd.setFecha(b.getFecha());
                    bd.setObservacion(b.getObservacion());
                    return bd;
                }).collect(Collectors.toList());
        dto.setBitacora(bitacora);

        return dto;
    }

    // =========================================================
    // MÓDULO 2 — Seguimiento de avances
    // =========================================================

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosEnPredefensa() {
        return proyectoRepo.findAll().stream()
                .filter(p -> "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstadoProyecto(p.getEstado());
                    if (p.getPropuesta() != null) {
                        if (p.getPropuesta().getEstudiante() != null) {
                            dto.setIdEstudiante(p.getPropuesta().getEstudiante().getIdEstudiante());
                            dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                        }
                        if (p.getPropuesta().getCarrera() != null)
                            dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
                    }
                    if (p.getPeriodo() != null)
                        dto.setPeriodo(p.getPeriodo().getDescripcion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosDirector(Integer idDirector) {
        return proyectoRepo.findByDirector_IdDocente(idDirector).stream()
                .filter(p -> "DESARROLLO".equalsIgnoreCase(p.getEstado())
                        || "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setIdEstudiante(p.getPropuesta().getEstudiante().getIdEstudiante());
                    dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                    dto.setEstadoProyecto(p.getEstado());
                    return dto;
                }).collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosTribunal(Integer idDocente) {
        return tribunalRepo.findByDocente_IdDocente(idDocente).stream()
                .map(tp -> tp.getProyecto())
                .filter(p -> "PREDEFENSA".equalsIgnoreCase(p.getEstado()))
                .distinct()
                .map(p -> {
                    Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
                    dto.setIdProyecto(p.getIdProyecto());
                    dto.setTitulo(p.getTitulo());
                    dto.setEstadoProyecto(p.getEstado());
                    if (p.getPropuesta() != null) {
                        if (p.getPropuesta().getEstudiante() != null) {
                            dto.setIdEstudiante(p.getPropuesta().getEstudiante().getIdEstudiante());
                            dto.setEstudiante(fullName(p.getPropuesta().getEstudiante().getUsuario()));
                        }
                        if (p.getPropuesta().getCarrera() != null)
                            dto.setCarrera(p.getPropuesta().getCarrera().getNombre());
                    }
                    if (p.getPeriodo() != null)
                        dto.setPeriodo(p.getPeriodo().getDescripcion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Dt2Dtos.ProyectoPendienteConfiguracionDto> listarProyectosDocenteDt2(Integer idDocenteDt2) {
        List<Dt2Asignacion> asignaciones = dt2AsignacionRepo.findByDocenteDt2_IdDocenteAndActivoTrue(idDocenteDt2);
        List<Dt2Dtos.ProyectoPendienteConfiguracionDto> resultado = new java.util.ArrayList<>();

        for (Dt2Asignacion asignacion : asignaciones) {
            ProyectoTitulacion proyecto = asignacion.getProyecto();
            if (proyecto == null) continue;

            boolean listo = documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())
                    .map(doc -> doc.getEstado() == EstadoDocumento.APROBADO_POR_DIRECTOR
                            || doc.getEstado() == EstadoDocumento.ANTIPLAGIO_APROBADO
                            || doc.getEstado() == EstadoDocumento.EN_PREDEFENSA
                            || doc.getEstado() == EstadoDocumento.LISTO_SUSTENTACION)
                    .orElse(false);
            if (!listo) continue;

            Dt2Dtos.ProyectoPendienteConfiguracionDto dto = new Dt2Dtos.ProyectoPendienteConfiguracionDto();
            dto.setIdProyecto(proyecto.getIdProyecto());
            dto.setTitulo(proyecto.getTitulo());
            dto.setEstadoProyecto(proyecto.getEstado());
            if (proyecto.getPropuesta() != null) {
                if (proyecto.getPropuesta().getEstudiante() != null) {
                    dto.setIdEstudiante(proyecto.getPropuesta().getEstudiante().getIdEstudiante());
                    dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
                }
                if (proyecto.getPropuesta().getCarrera() != null)
                    dto.setCarrera(proyecto.getPropuesta().getCarrera().getNombre());
            }
            if (proyecto.getPeriodo() != null)
                dto.setPeriodo(proyecto.getPeriodo().getDescripcion());

            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional
    public Dt2Dtos.AsesoriaDto registrarAsesoria(Dt2Dtos.RegistrarAsesoriaRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "DESARROLLO", "PREDEFENSA");

        Docente director = getDocente(req.getIdDirector());
        if (proyecto.getDirector() == null
                || !proyecto.getDirector().getIdDocente().equals(req.getIdDirector())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el Director asignado puede registrar asesorías");
        }
        if (req.getNumeroCorte() == null || (req.getNumeroCorte() != 1 && req.getNumeroCorte() != 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de corte debe ser 1 o 2");
        }

        AsesoriaDirector asesoria = new AsesoriaDirector();
        asesoria.setProyecto(proyecto);
        asesoria.setDirector(director);
        asesoria.setFecha(req.getFecha() != null ? req.getFecha() : LocalDateTime.now());
        asesoria.setObservaciones(req.getObservaciones());
        asesoria.setEvidenciaUrl(req.getEvidenciaUrl());
        asesoria.setPorcentajeAvance(req.getPorcentajeAvance());
        asesoria.setNumeroCorte(req.getNumeroCorte());
        asesoria.setCalificacion(req.getCalificacion());
        asesoriaRepo.save(asesoria);

        return mapAsesoria(asesoria);
    }

    @Transactional
    public List<Dt2Dtos.AsesoriaDto> listarAsesorias(Integer idProyecto) {
        return asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().map(this::mapAsesoria).collect(Collectors.toList());
    }

    @Transactional
    public Dt2Dtos.ActaCorteDto cerrarCorte(Dt2Dtos.CerrarCorteRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarEstadoProyecto(proyecto, "DESARROLLO");

        if (actaCorteRepo.existsByProyecto_IdProyectoAndNumeroCorte(req.getIdProyecto(), req.getNumeroCorte())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un acta para el corte " + req.getNumeroCorte());
        }

        validarPlazoCierreCorte(proyecto, req.getNumeroCorte());
        validarReunionesMensuales(req.getIdProyecto(), req.getNumeroCorte(), proyecto.getPeriodo());

        Docente director    = getDocente(req.getIdDirector());
        long totalAsesorias = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(req.getIdProyecto(), req.getNumeroCorte());

        if (totalAsesorias < MIN_ASESORIAS_CORTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cerrar el corte evaluativo: se requieren mínimo " + MIN_ASESORIAS_CORTE
                            + " asesorías registradas (actualmente: " + totalAsesorias + ")");
        }

        ActaCorte acta = new ActaCorte();
        acta.setProyecto(proyecto);
        acta.setNumeroCorte(req.getNumeroCorte());
        acta.setDirector(director);
        acta.setTotalAsesorias((int) totalAsesorias);
        acta.setAsesoriasSuficientes(true);
        acta.setNotaCorte(req.getNotaCorte());
        acta.setObservaciones(req.getObservaciones());
        actaCorteRepo.save(acta);

        return mapActaCorte(acta);
    }

    /**
     * Última semana del corte + margen de 5 días hábiles (Módulo 8). Si el
     * periodo no tiene configuradas las fechas del corte, se omite la
     * validación (campo nullable — ver PeriodoTitulacion).
     */
    private void validarPlazoCierreCorte(ProyectoTitulacion proyecto, Integer numeroCorte) {
        PeriodoTitulacion periodo = proyecto.getPeriodo();
        if (periodo == null) return;

        LocalDate fechaFinCorte = numeroCorte != null && numeroCorte == 1
                ? periodo.getFechaFinCorte1() : periodo.getFechaFinCorte2();
        if (fechaFinCorte == null) return;

        LocalDate limite = CalendarioAcademicoUtil.sumarDiasHabiles(fechaFinCorte, DIAS_GRACIA_CIERRE_CORTE);
        if (LocalDate.now().isAfter(limite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cerrar el corte: venció el plazo de envío (fin del corte " + fechaFinCorte
                            + " + " + DIAS_GRACIA_CIERRE_CORTE + " días hábiles = " + limite + ")");
        }
    }

    /**
     * Art. correspondiente del reglamento: cada mes del corte debe registrar
     * al menos 2 reuniones (asesorías). Se omite si el periodo no tiene
     * configurada la ventana del corte.
     */
    private void validarReunionesMensuales(Integer idProyecto, Integer numeroCorte, PeriodoTitulacion periodo) {
        if (periodo == null) return;
        LocalDate inicio = numeroCorte != null && numeroCorte == 1
                ? periodo.getFechaInicioCorte1() : periodo.getFechaInicioCorte2();
        LocalDate fin = numeroCorte != null && numeroCorte == 1
                ? periodo.getFechaFinCorte1() : periodo.getFechaFinCorte2();
        if (inicio == null || fin == null) return;

        List<AsesoriaDirector> asesorias =
                asesoriaRepo.findByProyecto_IdProyectoAndNumeroCorteOrderByFechaDesc(idProyecto, numeroCorte);

        YearMonth mesActual = YearMonth.from(inicio);
        YearMonth mesFin = YearMonth.from(fin);
        List<String> mesesIncumplidos = new ArrayList<>();
        while (!mesActual.isAfter(mesFin)) {
            YearMonth mes = mesActual;
            long enElMes = asesorias.stream()
                    .filter(a -> a.getFecha() != null && YearMonth.from(a.getFecha()).equals(mes))
                    .count();
            if (enElMes < MIN_REUNIONES_MENSUALES) {
                mesesIncumplidos.add(mesActual.toString());
            }
            mesActual = mesActual.plusMonths(1);
        }

        if (!mesesIncumplidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cerrar el corte: los siguientes meses no cumplen el mínimo de "
                            + MIN_REUNIONES_MENSUALES + " reuniones: " + String.join(", ", mesesIncumplidos));
        }
    }

    @Transactional
    public Dt2Dtos.SeguimientoDto getSeguimiento(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        long corte1 = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1);
        long corte2 = asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 2);

        BigDecimal ultimoAvance = asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)
                .stream().filter(a -> a.getPorcentajeAvance() != null)
                .findFirst().map(AsesoriaDirector::getPorcentajeAvance).orElse(BigDecimal.ZERO);

        List<Dt2Dtos.ActaCorteDto> actas = actaCorteRepo.findByProyecto_IdProyectoOrderByNumeroCorteAsc(idProyecto)
                .stream().map(this::mapActaCorte).collect(Collectors.toList());

        Dt2Dtos.SeguimientoDto dto = new Dt2Dtos.SeguimientoDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
        dto.setEstadoProyecto(proyecto.getEstado());
        dto.setTotalAsesorias((int) (corte1 + corte2));
        dto.setAsesoriaCorte1((int) corte1);
        dto.setAsesoriaCorte2((int) corte2);
        dto.setUltimoAvance(ultimoAvance);
        dto.setActas(actas);
        return dto;
    }

    // =========================================================
    // MÓDULO 3 — Certificación antiplagio
    // =========================================================

    @Transactional
    public Dt2Dtos.CertificadoAntiplacioDto registrarAntiplagio(
            Integer idProyecto, Integer idDocenteDt2,
            BigDecimal porcentajeCoincidencia, String observaciones,
            MultipartFile archivoPdf) {

        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        DocumentoTitulacion documento = documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para este proyecto"));

        if (documento.getEstado() != EstadoDocumento.APROBADO_POR_DIRECTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en APROBADO_POR_DIRECTOR. Estado actual: " + documento.getEstado());
        }

        Dt2Asignacion dt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay docente DT2 asignado al proyecto"));

        if (!dt2.getDocenteDt2().getIdDocente().equals(idDocenteDt2)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el Docente DT2 asignado puede subir el informe antiplagio");
        }

        Docente docenteDt2  = dt2.getDocenteDt2();
        String  urlInforme  = storageService.storeDocument(archivoPdf);
        boolean favorable   = porcentajeCoincidencia.compareTo(UMBRAL_ANTIPLAGIO) < 0;

        AntiplacioIntento intento = new AntiplacioIntento();
        intento.setProyecto(proyecto);
        intento.setDirector(docenteDt2);
        intento.setPorcentajeCoincidencia(porcentajeCoincidencia);
        intento.setUrlInforme(urlInforme);
        intento.setFavorable(favorable);
        intento.setObservaciones(observaciones);
        antiplacioRepo.save(intento);

        proyecto.setPorcentajeAntiplagio(porcentajeCoincidencia);
        proyecto.setFechaVerificacionAntiplagio(LocalDate.now());
        proyecto.setUrlInformeAntiplagio(urlInforme);

        if (favorable) {
            documento.setEstado(EstadoDocumento.ANTIPLAGIO_APROBADO);
            documentoRepo.save(documento);
            proyecto.setEstado("PREDEFENSA");
        }
        proyectoRepo.save(proyecto);

        return buildCertificadoDto(idProyecto);
    }

    @Transactional
    public Dt2Dtos.CertificadoAntiplacioDto getCertificadoAntiplagio(Integer idProyecto) {
        getProyecto(idProyecto);
        return buildCertificadoDto(idProyecto);
    }

    // =========================================================
    // MÓDULO 3B — Revisión IA del contenido del documento
    // =========================================================

    /**
     * Devuelve todos los campos de texto del DocumentoTitulacion
     * para que el frontend los envíe a WasItAIGenerated sección por sección.
     */
    @Transactional
    public Dt2Dtos.DocumentoTitulacionTextoDto getDocumentoTexto(Integer idProyecto) {
        DocumentoTitulacion doc = documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe documento de titulación para el proyecto: " + idProyecto));

        ProyectoTitulacion proyecto = doc.getProyecto();

        Dt2Dtos.DocumentoTitulacionTextoDto dto = new Dt2Dtos.DocumentoTitulacionTextoDto();
        dto.setIdDocumento(doc.getId());
        dto.setIdProyecto(idProyecto);
        dto.setTitulo(doc.getTitulo());
        dto.setEstadoDocumento(doc.getEstado() != null ? doc.getEstado().name() : null);
        dto.setEstadoRevisionIa(doc.getEstadoRevisionIa());
        dto.setFeedbackIa(doc.getFeedbackIa());

        // Nombre del estudiante
        if (doc.getEstudiante() != null && doc.getEstudiante().getUsuario() != null) {
            dto.setNombreEstudiante(fullName(doc.getEstudiante().getUsuario()));
        }
        // Carrera desde el proyecto
        if (proyecto != null
                && proyecto.getPropuesta() != null
                && proyecto.getPropuesta().getCarrera() != null) {
            dto.setCarreraEstudiante(proyecto.getPropuesta().getCarrera().getNombre());
        }

        // Secciones de texto
        dto.setResumen(doc.getResumen());
        dto.setAbstractText(doc.getAbstractText());
        dto.setIntroduccion(doc.getIntroduccion());
        dto.setPlanteamientoProblema(doc.getPlanteamientoProblema());
        dto.setJustificacion(doc.getJustificacion());
        dto.setObjetivoGeneral(doc.getObjetivoGeneral());
        dto.setObjetivosEspecificos(doc.getObjetivosEspecificos());
        dto.setMarcoTeorico(doc.getMarcoTeorico());
        dto.setMetodologia(doc.getMetodologia());
        dto.setResultados(doc.getResultados());
        dto.setDiscusion(doc.getDiscusion());
        dto.setConclusiones(doc.getConclusiones());
        dto.setRecomendaciones(doc.getRecomendaciones());
        dto.setBibliografia(doc.getBibliografia());
        dto.setAnexos(doc.getAnexos());

        return dto;
    }

    /**
     * Persiste el resultado del análisis IA en feedbackIa + estadoRevisionIa
     * del DocumentoTitulacion correspondiente al proyecto.
     */
    @Transactional
    public Dt2Dtos.MensajeDto guardarRevisionIa(Dt2Dtos.GuardarRevisionIaRequest req) {
        DocumentoTitulacion doc = documentoRepo.findByProyecto_IdProyecto(req.getIdProyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe documento de titulación para el proyecto: " + req.getIdProyecto()));

        String feedbackCompleto = String.format(
                "Porcentaje promedio IA: %d%% | Nivel de riesgo: %s | Detalle: %s",
                req.getPorcentajePromedioIa(),
                req.getNivelRiesgo(),
                req.getFeedbackIa() != null ? req.getFeedbackIa() : "Sin detalle"
        );

        doc.setFeedbackIa(feedbackCompleto);
        doc.setEstadoRevisionIa(req.getEstadoRevisionIa());
        doc.setFechaRevisionIa(LocalDateTime.now());
        documentoRepo.save(doc);

        return new Dt2Dtos.MensajeDto(
                "Análisis IA guardado correctamente. Nivel: " + req.getNivelRiesgo(),
                doc.getEstado().name(),
                true
        );
    }

    // =========================================================
    // MÓDULO 4 — Predefensa
    // =========================================================

    @Transactional
    public Dt2Dtos.MensajeDto programarPredefensa(Dt2Dtos.ProgramarPredefensaRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarAvalUnidadRegistro(proyecto);

        DocumentoTitulacion documento = documentoRepo.findByProyecto_IdProyecto(req.getIdProyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para este proyecto"));

        if (documento.getEstado() != EstadoDocumento.ANTIPLAGIO_APROBADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe tener antiplagio aprobado. Estado actual: " + documento.getEstado());
        }

        List<Sustentacion> existentes = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(req.getIdProyecto())
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .collect(Collectors.toList());

        Sustentacion sustentacion = existentes.isEmpty() ? new Sustentacion() : existentes.get(0);
        sustentacion.setProyecto(proyecto);
        sustentacion.setTipo("PREDEFENSA");
        sustentacion.setFecha(req.getFecha());
        sustentacion.setHora(req.getHora());
        sustentacion.setLugar(req.getLugar());
        sustentacion.setObservaciones(req.getObservaciones());
        sustentacionRepo.save(sustentacion);

        documento.setEstado(EstadoDocumento.EN_PREDEFENSA);
        documentoRepo.save(documento);

        return new Dt2Dtos.MensajeDto("Predefensa programada para el " + req.getFecha(), proyecto.getEstado(), true);
    }

    @Transactional
    public Dt2Dtos.PredefensaDto calificarPredefensaDocente(Dt2Dtos.CalificarPredefensaDocenteRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        validarEstadoDocumento(documento, EstadoDocumento.EN_PREDEFENSA);

        Dt2Asignacion dt2 = dt2AsignacionRepo.findByProyecto_IdProyectoAndActivoTrue(req.getIdProyecto())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay docente DT2 asignado al proyecto"));

        if (!dt2.getDocenteDt2().getIdDocente().equals(req.getIdDocenteDt2())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el docente DT2 asignado puede registrar esta calificación");
        }

        BigDecimal notaFinal = calcularNotaRubrica(req.getCalidadTrabajo(), req.getCapacidadCreadora(),
                req.getDominioExposicion(), req.getPertinenciaRespuestas());

        Sustentacion sus    = getPredefensa(req.getIdProyecto());
        Docente      docente = getDocente(req.getIdDocenteDt2());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocenteDt2(), "PREDEFENSA_DOCENTE")
                .orElse(new EvaluacionSustentacion());
        validarNoFinalizada(eval);

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("PREDEFENSA_DOCENTE");
        eval.setCalidadTrabajo(req.getCalidadTrabajo());
        eval.setOriginalidad(req.getCapacidadCreadora());
        eval.setDominioTema(req.getDominioExposicion());
        eval.setPreguntas(req.getPertinenciaRespuestas());
        eval.setNotaFinal(notaFinal);
        eval.setObservaciones(req.getObservaciones());
        evaluacionRepo.save(eval);

        return buildPredefensaDto(req.getIdProyecto(), sus);
    }

    @Transactional
    public Dt2Dtos.PredefensaDto calificarPredefensaTribunal(Dt2Dtos.CalificarPredefensaTribunalRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        validarEstadoDocumento(documento, EstadoDocumento.EN_PREDEFENSA);

        if (!tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(req.getIdProyecto(), req.getIdDocente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El docente no pertenece al tribunal de este proyecto");
        }

        BigDecimal notaFinal = calcularNotaRubrica(req.getCalidadTrabajo(), req.getCapacidadCreadora(),
                req.getDominioExposicion(), req.getPertinenciaRespuestas());

        Sustentacion sus    = getPredefensa(req.getIdProyecto());
        Docente      docente = getDocente(req.getIdDocente());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocente(), "PREDEFENSA_TRIBUNAL")
                .orElse(new EvaluacionSustentacion());
        validarNoFinalizada(eval);

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("PREDEFENSA_TRIBUNAL");
        eval.setCalidadTrabajo(req.getCalidadTrabajo());
        eval.setOriginalidad(req.getCapacidadCreadora());
        eval.setDominioTema(req.getDominioExposicion());
        eval.setPreguntas(req.getPertinenciaRespuestas());
        eval.setNotaFinal(notaFinal);
        eval.setObservaciones(req.getObservaciones());
        evaluacionRepo.save(eval);

        Dt2Dtos.PredefensaDto dto = buildPredefensaDto(req.getIdProyecto(), sus);

        if (Boolean.TRUE.equals(req.getSolicitudCorrecciones())) {
            documento.setEstado(EstadoDocumento.CORRECCION_REQUERIDA);
            documentoRepo.save(documento);
            dto.setSolicitudCorrecciones(true);
        } else {
            finalizarPredefensaSiCompleta(proyecto, sus, dto);
        }

        return dto;
    }

    /**
     * Art. 65: una vez que todo el panel (docente DT2 + tribunal) calificó
     * la predefensa sin solicitar correcciones, las calificaciones quedan
     * inapelables y la nota ponderada se registra formalmente como examen
     * final de Desarrollo de Titulación II (Módulo 8/9).
     */
    private void finalizarPredefensaSiCompleta(ProyectoTitulacion proyecto, Sustentacion sus, Dt2Dtos.PredefensaDto dto) {
        boolean todosCalificaron = dto.getNotaDocenteDt2() != null
                && dto.getMiembrosQueCalificaron() != null && dto.getTotalMiembrosTribunal() != null
                && dto.getMiembrosQueCalificaron() >= dto.getTotalMiembrosTribunal();
        if (!todosCalificaron) return;

        List<EvaluacionSustentacion> evalsTribunal = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_TRIBUNAL");
        evalsTribunal.forEach(e -> e.setCalificacionFinalizada(true));
        evaluacionRepo.saveAll(evalsTribunal);

        List<EvaluacionSustentacion> evalsDocente = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_DOCENTE");
        evalsDocente.forEach(e -> e.setCalificacionFinalizada(true));
        evaluacionRepo.saveAll(evalsDocente);

        if (dto.getNotaFinalPonderada() != null) {
            proyecto.setNotaPredefensaFinal(dto.getNotaFinalPonderada());
            proyectoRepo.save(proyecto);
        }
    }

    @Transactional
    public Dt2Dtos.PredefensaDto getPredefensaDto(Integer idProyecto) {
        getProyecto(idProyecto);
        Sustentacion sus = getPredefensaSafe(idProyecto);
        if (sus == null) return new Dt2Dtos.PredefensaDto();
        return buildPredefensaDto(idProyecto, sus);
    }

    // =========================================================
    // MÓDULO 5 — Sustentación final
    // =========================================================

    @Transactional
    public Dt2Dtos.DocumentosPreviosDto registrarDocumentosPrevios(Dt2Dtos.DocumentosPreviosRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.EN_PREDEFENSA
                && documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en EN_PREDEFENSA o LISTO_SUSTENTACION. Estado actual: " + documento.getEstado());
        }

        Usuario registradoPor = getUsuario(req.getIdRealizadoPor());

        DocumentoPrevioSustentacion docPrevio = docPrevioRepo.findByProyecto_IdProyecto(req.getIdProyecto())
                .orElse(new DocumentoPrevioSustentacion());

        docPrevio.setProyecto(proyecto);
        docPrevio.setEjemplarImpreso(Boolean.TRUE.equals(req.getEjemplarImpreso()));
        docPrevio.setCopiaDigitalBiblioteca(Boolean.TRUE.equals(req.getCopiaDigitalBiblioteca()));
        docPrevio.setCopiasDigitalesTribunal(Boolean.TRUE.equals(req.getCopiasDigitalesTribunal()));
        docPrevio.setInformeCompilatioFirmado(Boolean.TRUE.equals(req.getInformeCompilatioFirmado()));
        docPrevio.setObservaciones(req.getObservaciones());
        docPrevio.setRegistradoPor(registradoPor);
        docPrevioRepo.save(docPrevio);

        if (docPrevio.getCompleto() && documento.getEstado() == EstadoDocumento.EN_PREDEFENSA) {
            documento.setEstado(EstadoDocumento.LISTO_SUSTENTACION);
            documentoRepo.save(documento);
            proyecto.setEstado("SUSTENTACION");
            proyectoRepo.save(proyecto);
        }

        return mapDocumentosPrevios(docPrevio);
    }

    @Transactional
    public Dt2Dtos.DocumentosPreviosDto getDocumentosPrevios(Integer idProyecto) {
        return docPrevioRepo.findByProyecto_IdProyecto(idProyecto)
                .map(this::mapDocumentosPrevios)
                .orElse(new Dt2Dtos.DocumentosPreviosDto(false, false, false, false, false, null));
    }

    @Transactional
    public Dt2Dtos.MensajeDto programarSustentacion(Dt2Dtos.ProgramarSustentacionRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarAvalUnidadRegistro(proyecto);

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION. Estado actual: " + documento.getEstado());
        }

        if (!predefensaAprobada(req.getIdProyecto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La predefensa debe estar aprobada antes de programar la sustentación final");
        }

        List<Sustentacion> existentes = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(req.getIdProyecto())
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                .collect(Collectors.toList());

        Sustentacion sus = existentes.isEmpty() ? new Sustentacion() : existentes.get(0);
        sus.setProyecto(proyecto);
        sus.setTipo("DEFENSA_FINAL");
        sus.setFecha(req.getFecha());
        sus.setHora(req.getHora());
        sus.setLugar(req.getLugar());
        sus.setObservaciones(req.getObservaciones());
        sustentacionRepo.save(sus);

        return new Dt2Dtos.MensajeDto("Sustentación final programada para el " + req.getFecha(), proyecto.getEstado(), true);
    }

    @Transactional
    public Dt2Dtos.ResultadoSustentacionDto calificarSustentacion(Dt2Dtos.CalificarSustentacionRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());

        DocumentoTitulacion documento = getDocumentoRequerido(req.getIdProyecto());
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION. Estado actual: " + documento.getEstado());
        }

        if (!tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(req.getIdProyecto(), req.getIdDocente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El docente no pertenece al tribunal de este proyecto");
        }

        validarNota(req.getCalidadTrabajo(), "calidad del trabajo");
        validarNota(req.getOriginalidad(),   "originalidad");
        validarNota(req.getDominioTema(),    "dominio del tema");
        validarNota(req.getPreguntas(),      "pertinencia de respuestas");

        BigDecimal notaFinal = req.getCalidadTrabajo().multiply(new BigDecimal("0.20"))
                .add(req.getOriginalidad().multiply(new BigDecimal("0.20")))
                .add(req.getDominioTema().multiply(new BigDecimal("0.30")))
                .add(req.getPreguntas().multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        Sustentacion sus     = getDefensaFinal(req.getIdProyecto());
        Docente      docente = getDocente(req.getIdDocente());

        EvaluacionSustentacion eval = evaluacionRepo
                .findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(
                        sus.getIdSustentacion(), req.getIdDocente(), "SUSTENTACION")
                .orElse(new EvaluacionSustentacion());
        validarNoFinalizada(eval);

        eval.setSustentacion(sus);
        eval.setDocente(docente);
        eval.setTipo("SUSTENTACION");
        eval.setCalidadTrabajo(req.getCalidadTrabajo());
        eval.setOriginalidad(req.getOriginalidad());
        eval.setDominioTema(req.getDominioTema());
        eval.setPreguntas(req.getPreguntas());
        eval.setNotaFinal(notaFinal);
        eval.setObservaciones(req.getObservaciones());
        // Art. 65: la calificación es inapelable desde el momento en que se
        // emite, no solo a partir de la consolidación del tribunal completo.
        eval.setCalificacionFinalizada(true);
        evaluacionRepo.save(eval);

        return buildResultadoSustentacion(req.getIdProyecto(), sus, proyecto);
    }

    @Transactional
    public Dt2Dtos.ResultadoSustentacionDto consolidarResultado(Integer idProyecto) {
        ProyectoTitulacion proyecto = getProyecto(idProyecto);

        DocumentoTitulacion documento = getDocumentoRequerido(idProyecto);
        if (documento.getEstado() != EstadoDocumento.LISTO_SUSTENTACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en LISTO_SUSTENTACION para consolidar.");
        }

        Sustentacion sus         = getDefensaFinal(idProyecto);
        long totalMiembros       = tribunalRepo.countByProyecto_IdProyecto(idProyecto);
        long miembrosCalificaron = evaluacionRepo.countBySustentacion_IdSustentacionAndTipo(
                sus.getIdSustentacion(), "SUSTENTACION");

        if (miembrosCalificaron < totalMiembros) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan " + (totalMiembros - miembrosCalificaron) + " miembros del tribunal por calificar");
        }

        // Art. 65: con el panel completo, las calificaciones quedan inapelables.
        List<EvaluacionSustentacion> evalsSustentacion = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "SUSTENTACION");
        evalsSustentacion.forEach(e -> e.setCalificacionFinalizada(true));
        evaluacionRepo.saveAll(evalsSustentacion);

        Dt2Dtos.ResultadoSustentacionDto resultado = buildResultadoSustentacion(idProyecto, sus, proyecto);

        BigDecimal notaSustentacion = resultado.getPromedioTribunal();
        boolean    aprobado         = notaSustentacion.compareTo(NOTA_APROBATORIA_SUSTENTACION) >= 0;

        Estudiante estudiante     = proyecto.getPropuesta().getEstudiante();
        if (estudiante.getPromedioRecord80() == null) {
            throw new IllegalStateException(
                    "No se puede calcular la nota de grado: el promedio del récord académico " +
                    "no ha sido registrado para el estudiante " + estudiante.getIdEstudiante());
        }
        BigDecimal promedioRecord = estudiante.getPromedioRecord80();
        validarPromedioAcademico(promedioRecord);
        BigDecimal notaGrado = promedioRecord.multiply(new BigDecimal("0.80"))
                .add(notaSustentacion.multiply(new BigDecimal("0.20")))
                .setScale(2, RoundingMode.HALF_UP);

        ActaGrado acta = actaGradoRepo.findByEstudiante_IdEstudiante(estudiante.getIdEstudiante())
                .orElse(new ActaGrado());
        acta.setEstudiante(estudiante);
        acta.setTipoTitulacion("TRABAJO_INTEGRACION_CURRICULAR");
        acta.setIdOrigen(proyecto.getIdProyecto());
        acta.setNotaRecord(promedioRecord);
        acta.setNotaTitulacion(notaSustentacion);
        acta.setNotaFinal(notaGrado);
        actaGradoRepo.save(acta);

        proyecto.setEstado(aprobado ? "FINALIZADO" : "REPROBADO");
        proyectoRepo.save(proyecto);

        documento.setEstado(aprobado
                ? EstadoDocumento.CERRADO_APROBADO
                : EstadoDocumento.CERRADO_REPROBADO);
        documentoRepo.save(documento);

        resultado.setNotaGradoFinal(notaGrado);
        resultado.setPromedioRecordAcademico(promedioRecord);
        resultado.setResultado(aprobado ? "APROBADO" : "REPROBADO");
        resultado.setHabilitadoSegundaOportunidad(!aprobado);

        if (!aprobado) {
            resultado.setFechaLimiteSegundaOportunidad(
                    sus.getFecha().plusDays(DIAS_SEGUNDA_OPORTUNIDAD));
        }

        registrarOportunidadTic(proyecto, estudiante, sus, notaSustentacion, aprobado);

        return resultado;
    }

    /** Módulo 13 — cada consolidación de sustentación genera un RegistroOportunidad. */
    private void registrarOportunidadTic(ProyectoTitulacion proyecto, Estudiante estudiante,
                                          Sustentacion sus, BigDecimal notaSustentacion, boolean aprobado) {
        if (proyecto.getPeriodo() == null) return;

        long intentosDefensaFinal = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(proyecto.getIdProyecto())
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo())).count();
        com.erwin.backend.enums.TipoOportunidad tipoOportunidad = intentosDefensaFinal <= 1
                ? com.erwin.backend.enums.TipoOportunidad.FINAL
                : com.erwin.backend.enums.TipoOportunidad.SUPLETORIO;

        oportunidadService.registrarResultado(
                estudiante, proyecto.getPeriodo(),
                com.erwin.backend.enums.ModalidadOportunidad.TRABAJO_INTEGRACION_CURRICULAR,
                tipoOportunidad,
                aprobado ? com.erwin.backend.enums.ResultadoOportunidad.APROBADO
                        : com.erwin.backend.enums.ResultadoOportunidad.REPROBADO,
                notaSustentacion, sus.getFecha(),
                "Sustentación final consolidada — proyecto " + proyecto.getIdProyecto());
    }

    @Transactional
    public Dt2Dtos.MensajeDto habilitarSegundaOportunidad(Dt2Dtos.SegundaOportunidadRequest req) {
        ProyectoTitulacion proyecto = getProyecto(req.getIdProyecto());
        validarAvalUnidadRegistro(proyecto);
        validarEstadoProyecto(proyecto, "REPROBADO");

        if (req.getFechaSustentacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar la fecha de la nueva sustentación");
        }

        // Art. 12: la segunda sustentación debe programarse dentro de los 15 días
        // posteriores a la primera sustentación reprobada.
        Sustentacion primeraReprobada = getDefensaFinal(req.getIdProyecto());
        LocalDate fechaLimite = primeraReprobada.getFecha().plusDays(DIAS_SEGUNDA_OPORTUNIDAD);
        if (req.getFechaSustentacion().isBefore(primeraReprobada.getFecha())
                || req.getFechaSustentacion().isAfter(fechaLimite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La segunda sustentación debe programarse dentro de los " + DIAS_SEGUNDA_OPORTUNIDAD
                            + " días posteriores a la sustentación reprobada (" + primeraReprobada.getFecha()
                            + "); el plazo venció el " + fechaLimite + " — Art. 12 del Reglamento");
        }

        Sustentacion sus2 = new Sustentacion();
        sus2.setProyecto(proyecto);
        sus2.setTipo("DEFENSA_FINAL");
        sus2.setFecha(req.getFechaSustentacion());
        sus2.setHora(req.getHora());
        sus2.setLugar(req.getLugar());
        sus2.setObservaciones("SEGUNDA OPORTUNIDAD — " + (req.getObservaciones() != null ? req.getObservaciones() : ""));
        sustentacionRepo.save(sus2);

        documentoRepo.findByProyecto_IdProyecto(req.getIdProyecto()).ifPresent(doc -> {
            doc.setEstado(EstadoDocumento.LISTO_SUSTENTACION);
            documentoRepo.save(doc);
        });

        proyecto.setEstado("SUSTENTACION");
        proyectoRepo.save(proyecto);

        return new Dt2Dtos.MensajeDto(
                "Segunda oportunidad programada para el " + req.getFechaSustentacion(),
                proyecto.getEstado(), true);
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    private ProyectoTitulacion getProyecto(Integer idProyecto) {
        return proyectoRepo.findById(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proyecto no encontrado: " + idProyecto));
    }

    /** Módulo 18, TAREA 3.2: las actividades del calendario solo se ejecutan con aval de Unidad de Registro. */
    private void validarAvalUnidadRegistro(ProyectoTitulacion proyecto) {
        PeriodoTitulacion periodo = proyecto.getPeriodo();
        if (periodo != null && !Boolean.TRUE.equals(periodo.getAvalUnidadRegistro())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede programar: el periodo " + periodo.getDescripcion()
                            + " todavía no tiene el aval de la Unidad de Registro");
        }
    }

    private Docente getDocente(Integer idDocente) {
        return docenteRepo.findById(idDocente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Docente no encontrado: " + idDocente));
    }

    private Usuario getUsuario(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado: " + idUsuario));
    }

    private DocumentoTitulacion getDocumentoRequerido(Integer idProyecto) {
        return documentoRepo.findByProyecto_IdProyecto(idProyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No existe documento de titulación para el proyecto: " + idProyecto));
    }

    private void validarEstadoDocumento(DocumentoTitulacion documento, EstadoDocumento estadoEsperado) {
        if (documento.getEstado() != estadoEsperado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento debe estar en estado " + estadoEsperado
                            + ". Estado actual: " + documento.getEstado());
        }
    }

    private void validarEstadoProyecto(ProyectoTitulacion p, String... estadosValidos) {
        for (String estado : estadosValidos) {
            if (estado.equalsIgnoreCase(p.getEstado())) return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El proyecto está en estado '" + p.getEstado() + "'. Estados válidos: "
                        + String.join(", ", estadosValidos));
    }

    private void validarAnteproyectoAprobado(ProyectoTitulacion proyecto) {
        if ("DESARROLLO".equalsIgnoreCase(proyecto.getEstado())) return;

        AnteproyectoTitulacion ante = anteproyectoRepo
                .findByPropuesta_IdPropuesta(proyecto.getPropuesta().getIdPropuesta())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proyecto no tiene anteproyecto registrado"));
        if (ante.getEstado() != EstadoAnteproyecto.APROBADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El anteproyecto debe estar APROBADO. Estado actual: " + ante.getEstado());
        }
    }

    private void intentarTransicionDesarrollo(ProyectoTitulacion proyecto) {
        if (!"ANTEPROYECTO".equalsIgnoreCase(proyecto.getEstado())) return;

        boolean tieneDt2      = dt2AsignacionRepo.existsByProyecto_IdProyecto(proyecto.getIdProyecto());
        boolean tieneDirector = proyecto.getDirector() != null;
        boolean tieneTribunal = tribunalRepo.countByProyecto_IdProyecto(proyecto.getIdProyecto()) >= MIN_MIEMBROS_TRIBUNAL;

        if (tieneDt2 && tieneDirector && tieneTribunal) {
            proyecto.setEstado("DESARROLLO");
            proyectoRepo.save(proyecto);

            documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto()).orElseGet(() -> {
                DocumentoTitulacion doc = new DocumentoTitulacion();
                doc.setProyecto(proyecto);
                doc.setEstudiante(proyecto.getPropuesta().getEstudiante());
                doc.setDirector(proyecto.getDirector());
                doc.setEstado(EstadoDocumento.BORRADOR);
                doc.setTitulo(proyecto.getTitulo());
                return documentoRepo.save(doc);
            });

            try {
                Estudiante est      = proyecto.getPropuesta().getEstudiante();
                String     emailEst = est != null && est.getUsuario() != null
                        ? est.getUsuario().getCorreoInstitucional() : null;
                if (emailEst != null && !emailEst.isBlank()) {
                    String nombreEst      = fullName(est.getUsuario());
                    String periodo        = proyecto.getPeriodo() != null ? proyecto.getPeriodo().getDescripcion() : "";
                    String nombreDirector = proyecto.getDirector() != null
                            ? fullName(proyecto.getDirector().getUsuario()) : null;
                    String nombreDt2 = dt2AsignacionRepo
                            .findByProyecto_IdProyectoAndActivoTrue(proyecto.getIdProyecto())
                            .map(a -> fullName(a.getDocenteDt2().getUsuario()))
                            .orElse(null);
                    emailService.notificarAsignacionEquipo(
                            emailEst, nombreEst, proyecto.getTitulo(),
                            nombreDirector, nombreDt2, periodo);
                }
            } catch (Exception e) {
                System.err.println("Error al notificar asignacion equipo: " + e.getMessage());
            }
        }
    }

    private void registrarBitacora(ProyectoTitulacion proyecto, String tipo, Integer idAsignado,
                                   String nombre, String cargo, Usuario realizadoPor,
                                   String periodo, String observacion) {
        BitacoraAsignacion b = new BitacoraAsignacion();
        b.setProyecto(proyecto);
        b.setTipoAsignacion(tipo);
        b.setIdAsignado(idAsignado);
        b.setNombreAsignado(nombre);
        b.setCargo(cargo);
        b.setRealizadoPor(realizadoPor);
        b.setPeriodo(periodo);
        b.setObservacion(observacion);
        bitacoraRepo.save(b);
    }

    private String fullName(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres()   != null ? u.getNombres()   : "") + " "
                + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private void validarNota(BigDecimal nota, String campo) {
        if (nota == null || nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(BigDecimal.TEN) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La " + campo + " debe estar entre 0 y 10");
        }
    }

    /**
     * Rúbrica 20/20/30/30 (calidad del trabajo, capacidad creadora, dominio de
     * la exposición, pertinencia de las respuestas), cada indicador calificado
     * sobre 10 por el evaluador — misma ponderación que la sustentación final.
     */
    private BigDecimal calcularNotaRubrica(BigDecimal calidadTrabajo, BigDecimal capacidadCreadora,
                                           BigDecimal dominioExposicion, BigDecimal pertinenciaRespuestas) {
        validarNota(calidadTrabajo, "calidad del trabajo");
        validarNota(capacidadCreadora, "capacidad creadora");
        validarNota(dominioExposicion, "dominio de la exposición");
        validarNota(pertinenciaRespuestas, "pertinencia de las respuestas");

        return calidadTrabajo.multiply(new BigDecimal("0.20"))
                .add(capacidadCreadora.multiply(new BigDecimal("0.20")))
                .add(dominioExposicion.multiply(new BigDecimal("0.30")))
                .add(pertinenciaRespuestas.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Art. 65 del Reglamento: una calificación finalizada es inapelable. */
    private void validarNoFinalizada(EvaluacionSustentacion eval) {
        if (Boolean.TRUE.equals(eval.getCalificacionFinalizada())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La calificación es inapelable una vez emitida — Art. 65 del Reglamento");
        }
    }

    private void validarPromedioAcademico(BigDecimal promedio) {
        if (promedio.compareTo(BigDecimal.ZERO) < 0 || promedio.compareTo(BigDecimal.TEN) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El promedio del récord académico debe estar entre 0.0 y 10.0 (valor recibido: " + promedio + ")");
        }
    }

    private Sustentacion getPredefensa(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay predefensa programada para este proyecto"));
    }

    private Sustentacion getPredefensaSafe(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "PREDEFENSA".equalsIgnoreCase(s.getTipo()))
                .findFirst().orElse(null);
    }

    private Sustentacion getDefensaFinal(Integer idProyecto) {
        return sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No hay sustentación final programada para este proyecto"));
    }

    private boolean predefensaAprobada(Integer idProyecto) {
        Sustentacion sus = getPredefensaSafe(idProyecto);
        if (sus == null) return false;
        return "APROBADA".equalsIgnoreCase(buildPredefensaDto(idProyecto, sus).getEstado());
    }

    private Dt2Dtos.PredefensaDto buildPredefensaDto(Integer idProyecto, Sustentacion sus) {
        Dt2Dtos.PredefensaDto dto = new Dt2Dtos.PredefensaDto();
        dto.setIdSustentacion(sus.getIdSustentacion());
        dto.setFecha(sus.getFecha());
        dto.setHora(sus.getHora());
        dto.setLugar(sus.getLugar());

        evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_DOCENTE")
                .stream().findFirst().ifPresent(e -> dto.setNotaDocenteDt2(e.getNotaFinal()));

        List<EvaluacionSustentacion> evalsTribunal = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "PREDEFENSA_TRIBUNAL");

        List<Dt2Dtos.EvaluacionMiembroDto> evalsDto = evalsTribunal.stream().map(e -> {
            Dt2Dtos.EvaluacionMiembroDto m = new Dt2Dtos.EvaluacionMiembroDto();
            m.setIdDocente(e.getDocente() != null ? e.getDocente().getIdDocente() : null);
            m.setNombreDocente(e.getDocente() != null ? fullName(e.getDocente().getUsuario()) : "");
            m.setNota(e.getNotaFinal());
            m.setObservaciones(e.getObservaciones());
            return m;
        }).collect(Collectors.toList());
        dto.setEvaluacionesTribunal(evalsDto);

        long totalMiembros = tribunalRepo.countByProyecto_IdProyecto(idProyecto);
        dto.setTotalMiembrosTribunal((int) totalMiembros);
        dto.setMiembrosQueCalificaron(evalsDto.size());

        if (!evalsTribunal.isEmpty()) {
            BigDecimal sumaT = evalsTribunal.stream()
                    .map(e -> e.getNotaFinal() != null ? e.getNotaFinal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promT = sumaT.divide(BigDecimal.valueOf(evalsTribunal.size()), 2, RoundingMode.HALF_UP);
            dto.setPromedioTribunal(promT);

            if (dto.getNotaDocenteDt2() != null) {
                BigDecimal ponderada = dto.getNotaDocenteDt2().multiply(new BigDecimal("0.60"))
                        .add(promT.multiply(new BigDecimal("0.40")))
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setNotaFinalPonderada(ponderada);

                boolean todasCalificaron = evalsDto.size() >= totalMiembros;
                boolean hayCorrecciones  = evalsDto.stream()
                        .anyMatch(e -> Boolean.TRUE.equals(e.getSolicitudCorrecciones()));

                if (hayCorrecciones) {
                    dto.setEstado("CON_OBSERVACIONES");
                    dto.setSolicitudCorrecciones(true);
                } else if (todasCalificaron) {
                    dto.setEstado(ponderada.compareTo(NOTA_APROBATORIA_PREDEFENSA) >= 0 ? "APROBADA" : "CON_OBSERVACIONES");
                } else {
                    dto.setEstado("EN_PROCESO");
                }
            } else {
                dto.setEstado("EN_PROCESO");
            }
        } else {
            dto.setEstado(dto.getNotaDocenteDt2() != null ? "EN_PROCESO" : "PENDIENTE");
        }

        return dto;
    }

    private Dt2Dtos.ResultadoSustentacionDto buildResultadoSustentacion(
            Integer idProyecto, Sustentacion sus, ProyectoTitulacion proyecto) {

        List<EvaluacionSustentacion> evals = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sus.getIdSustentacion(), "SUSTENTACION");

        List<Dt2Dtos.EvaluacionMiembroDto> evalsDto = evals.stream().map(e -> {
            Dt2Dtos.EvaluacionMiembroDto m = new Dt2Dtos.EvaluacionMiembroDto();
            m.setIdDocente(e.getDocente() != null ? e.getDocente().getIdDocente() : null);
            m.setNombreDocente(e.getDocente() != null ? fullName(e.getDocente().getUsuario()) : "");
            m.setNota(e.getNotaFinal());
            m.setObservaciones(e.getObservaciones());
            return m;
        }).collect(Collectors.toList());

        BigDecimal promedio = BigDecimal.ZERO;
        if (!evals.isEmpty()) {
            BigDecimal suma = evals.stream()
                    .map(e -> e.getNotaFinal() != null ? e.getNotaFinal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            promedio = suma.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
        }

        Dt2Dtos.ResultadoSustentacionDto dto = new Dt2Dtos.ResultadoSustentacionDto();
        dto.setIdProyecto(idProyecto);
        dto.setTitulo(proyecto.getTitulo());
        dto.setEstudiante(fullName(proyecto.getPropuesta().getEstudiante().getUsuario()));
        dto.setFechaSustentacion(sus.getFecha());
        dto.setPromedioTribunal(promedio);
        dto.setEvaluaciones(evalsDto);
        dto.setResultado("PENDIENTE_CONSOLIDACION");
        return dto;
    }

    private Dt2Dtos.CertificadoAntiplacioDto buildCertificadoDto(Integer idProyecto) {
        List<AntiplacioIntento> historial = antiplacioRepo
                .findByProyecto_IdProyectoOrderByFechaIntentoDesc(idProyecto);

        boolean           favorable = historial.stream().anyMatch(AntiplacioIntento::getFavorable);
        AntiplacioIntento ultimo    = historial.isEmpty() ? null : historial.get(0);

        List<Dt2Dtos.AntiplacioIntentoDto> historialDto = historial.stream().map(i -> {
            Dt2Dtos.AntiplacioIntentoDto d = new Dt2Dtos.AntiplacioIntentoDto();
            d.setIdIntento(i.getIdIntento());
            d.setFechaIntento(i.getFechaIntento());
            d.setPorcentajeCoincidencia(i.getPorcentajeCoincidencia());
            d.setUrlInforme(i.getUrlInforme());
            d.setFavorable(i.getFavorable());
            d.setObservaciones(i.getObservaciones());
            d.setResultado(Boolean.TRUE.equals(i.getFavorable())
                    ? "APROBADO (" + i.getPorcentajeCoincidencia() + "% < 10%)"
                    : "RECHAZADO (" + i.getPorcentajeCoincidencia() + "% >= 10%)");
            return d;
        }).collect(Collectors.toList());

        Dt2Dtos.CertificadoAntiplacioDto dto = new Dt2Dtos.CertificadoAntiplacioDto();
        dto.setIdProyecto(idProyecto);
        dto.setCertificadoFavorable(favorable);
        dto.setTotalIntentos(historial.size());
        dto.setHistorial(historialDto);
        if (ultimo != null) {
            dto.setUltimoPorcentaje(ultimo.getPorcentajeCoincidencia());
            dto.setUrlUltimoInforme(ultimo.getUrlInforme());
            dto.setFechaUltimoIntento(ultimo.getFechaIntento());
        }
        return dto;
    }

    private Dt2Dtos.AsesoriaDto mapAsesoria(AsesoriaDirector a) {
        Dt2Dtos.AsesoriaDto dto = new Dt2Dtos.AsesoriaDto();
        dto.setIdAsesoria(a.getIdAsesoria());
        dto.setFecha(a.getFecha());
        dto.setObservaciones(a.getObservaciones());
        dto.setEvidenciaUrl(a.getEvidenciaUrl());
        dto.setPorcentajeAvance(a.getPorcentajeAvance());
        dto.setNumeroCorte(a.getNumeroCorte());
        dto.setCalificacion(a.getCalificacion());
        return dto;
    }

    private Dt2Dtos.ActaCorteDto mapActaCorte(ActaCorte a) {
        Dt2Dtos.ActaCorteDto dto = new Dt2Dtos.ActaCorteDto();
        dto.setIdActaCorte(a.getIdActaCorte());
        dto.setNumeroCorte(a.getNumeroCorte());
        dto.setFechaGeneracion(a.getFechaGeneracion());
        dto.setTotalAsesorias(a.getTotalAsesorias());
        dto.setAsesoriasSuficientes(a.getAsesoriasSuficientes());
        dto.setNotaCorte(a.getNotaCorte());
        dto.setObservaciones(a.getObservaciones());
        dto.setUrlActaPdf(a.getUrlActaPdf());
        return dto;
    }

    private Dt2Dtos.DocumentosPreviosDto mapDocumentosPrevios(DocumentoPrevioSustentacion d) {
        return new Dt2Dtos.DocumentosPreviosDto(
                d.getEjemplarImpreso(), d.getCopiaDigitalBiblioteca(),
                d.getCopiasDigitalesTribunal(), d.getInformeCompilatioFirmado(),
                d.getCompleto(), d.getObservaciones());
    }
}