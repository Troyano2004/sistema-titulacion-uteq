package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.JustificacionDtos.*;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.entities.SolicitudJustificacion;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.*;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.FeriadoAcademicoRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import com.erwin.backend.repository.SolicitudJustificacionRepository;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Módulo 14 — justificaciones y casos especiales. Los plazos son estrictos
 * (Art. reglamentario): una solicitud presentada después de su fechaLimite
 * se rechaza directamente, no se registra "a tiempo pero tarde".
 */
@Service
@Transactional
public class JustificacionService {

    private static final Logger log = LoggerFactory.getLogger(JustificacionService.class);

    private static final int HORAS_PLAZO_INASISTENCIA_COMPLEXIVO = 72;
    private static final int DIAS_HABILES_PLAZO_INASISTENCIA_SUSTENTACION = 3;
    private static final int DIAS_HOSPITALIZACION_MINIMOS_CASO_MEDICO = 3;
    private static final int SEMANAS_REPOSO_CASO_MEDICO = 3;

    private final SolicitudJustificacionRepository repo;
    private final EstudianteRepository estudianteRepo;
    private final UsuarioRepository usuarioRepo;
    private final FeriadoAcademicoRepository feriadoRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final OportunidadService oportunidadService;
    private final AuthorizationService authorizationService;

    public JustificacionService(SolicitudJustificacionRepository repo,
                                 EstudianteRepository estudianteRepo,
                                 UsuarioRepository usuarioRepo,
                                 FeriadoAcademicoRepository feriadoRepo,
                                 PeriodoTitulacionRepository periodoRepo,
                                 OportunidadService oportunidadService,
                                 AuthorizationService authorizationService) {
        this.repo = repo;
        this.estudianteRepo = estudianteRepo;
        this.usuarioRepo = usuarioRepo;
        this.feriadoRepo = feriadoRepo;
        this.periodoRepo = periodoRepo;
        this.oportunidadService = oportunidadService;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Solicitar (TAREA 2.2 / 2.3)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "SolicitudJustificacion", accion = "SOLICITAR")
    public SolicitudJustificacionDto solicitar(SolicitarJustificacionRequest req) {
        authorizationService.exigirPropioEstudianteSiAplica(req.idEstudiante());
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));

        TipoJustificacion tipo = parseTipo(req.tipo());
        if (req.motivo() == null || req.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }
        if (req.fechaEvento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha del evento a justificar es obligatoria");
        }

        if (tipo == TipoJustificacion.CASO_MEDICO_GRAVE) {
            if (repo.existsByEstudiante_IdEstudianteAndTipo(req.idEstudiante(), TipoJustificacion.CASO_MEDICO_GRAVE)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe una solicitud de caso médico grave para este estudiante — solo se permite una");
            }
            validarDocumentoCasoMedico(req);
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite = calcularFechaLimite(tipo, req.fechaEvento());

        if (fechaLimite != null && ahora.isAfter(fechaLimite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fuera de plazo: la solicitud debía presentarse antes del " + fechaLimite);
        }

        SolicitudJustificacion solicitud = new SolicitudJustificacion();
        solicitud.setEstudiante(estudiante);
        solicitud.setTipo(tipo);
        solicitud.setMotivo(req.motivo());
        solicitud.setDocumentoAdjuntoId(req.documentoAdjuntoId());
        solicitud.setTipoDocumento(parseTipoDocumentoOpcional(req.tipoDocumento()));
        solicitud.setFechaEvento(req.fechaEvento());
        solicitud.setFechaSolicitud(ahora);
        solicitud.setFechaLimite(fechaLimite);
        solicitud.setEstado(EstadoJustificacion.PENDIENTE);

        return toDto(repo.save(solicitud));
    }

    private LocalDateTime calcularFechaLimite(TipoJustificacion tipo, LocalDateTime fechaEvento) {
        Set<java.time.LocalDate> feriados = feriadoRepo.fechasComoSet();
        return switch (tipo) {
            case INASISTENCIA_COMPLEXIVO -> CalendarioAcademicoUtil.sumarHorasLaborables(
                    fechaEvento, HORAS_PLAZO_INASISTENCIA_COMPLEXIVO, feriados);
            case INASISTENCIA_SUSTENTACION -> CalendarioAcademicoUtil
                    .sumarDiasHabiles(fechaEvento.toLocalDate(), DIAS_HABILES_PLAZO_INASISTENCIA_SUSTENTACION, feriados)
                    .atTime(23, 59);
            case CASO_MEDICO_GRAVE -> null; // sin vencimiento automático — ver reglas 2.2
        };
    }

    private void validarDocumentoCasoMedico(SolicitarJustificacionRequest req) {
        if (req.tipoDocumento() == null || req.tipoDocumento().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El caso médico grave requiere indicar el tipo de certificado (IESS, MSP o privado validado)");
        }
        TipoDocumentoJustificacion tipoDoc = parseTipoDocumentoOpcional(req.tipoDocumento());
        if (tipoDoc == TipoDocumentoJustificacion.CERTIFICADO_MEDICO_PRIVADO) {
            log.info("Caso médico grave con certificado privado — requiere validación posterior por IESS o MSP (estudiante {})",
                    req.idEstudiante());
        }
        if (req.documentoAdjuntoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El caso médico grave requiere adjuntar el certificado médico (mínimo 3 días de hospitalización y "
                            + SEMANAS_REPOSO_CASO_MEDICO + " semanas de reposo)");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Resolver (DECANO)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "SolicitudJustificacion", accion = "RESOLVER")
    public SolicitudJustificacionDto resolver(Long id, ResolverJustificacionRequest req) {
        SolicitudJustificacion solicitud = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOLICITUD_NO_ENCONTRADA"));

        if (solicitud.getEstado() != EstadoJustificacion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La solicitud ya fue resuelta o venció (" + solicitud.getEstado() + ")");
        }

        EstadoJustificacion nuevoEstado;
        try {
            nuevoEstado = EstadoJustificacion.valueOf(req.estado().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + req.estado());
        }
        if (nuevoEstado != EstadoJustificacion.APROBADA && nuevoEstado != EstadoJustificacion.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser APROBADA o RECHAZADA");
        }

        Usuario resuelve = usuarioRepo.findById(req.idUsuarioResuelve())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        solicitud.setEstado(nuevoEstado);
        solicitud.setResueltaPor(resuelve);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setObservacionesResolucion(req.observacionesResolucion());
        if (nuevoEstado == EstadoJustificacion.APROBADA) {
            solicitud.setNuevaFechaProgramada(req.nuevaFechaProgramada());
        }

        return toDto(repo.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudJustificacionDto> historial(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return repo.findByEstudiante_IdEstudianteOrderByFechaSolicitudDesc(idEstudiante)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudJustificacionDto> pendientes() {
        return repo.findByEstadoOrderByFechaSolicitudAsc(EstadoJustificacion.PENDIENTE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // Scheduler diario — marcar VENCIDA y registrar REPROBADO (TAREA 2.4)
    // ═══════════════════════════════════════════════════════════════
    public List<Long> marcarVencidas() {
        List<SolicitudJustificacion> vencidas = repo.findByEstadoAndFechaLimiteBefore(
                EstadoJustificacion.PENDIENTE, LocalDateTime.now());

        List<Long> ids = new java.util.ArrayList<>();
        for (SolicitudJustificacion s : vencidas) {
            s.setEstado(EstadoJustificacion.VENCIDA);
            repo.save(s);
            ids.add(s.getId());
            registrarReprobadoPorVencimiento(s);
        }
        return ids;
    }

    private void registrarReprobadoPorVencimiento(SolicitudJustificacion s) {
        PeriodoTitulacion periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc().orElse(null);
        if (periodo == null) return;

        ModalidadOportunidad modalidad = switch (s.getTipo()) {
            case INASISTENCIA_COMPLEXIVO -> ModalidadOportunidad.EXAMEN_COMPLEXIVO;
            case INASISTENCIA_SUSTENTACION -> ModalidadOportunidad.TRABAJO_INTEGRACION_CURRICULAR;
            case CASO_MEDICO_GRAVE -> null;
        };
        if (modalidad == null) return;

        oportunidadService.registrarResultado(
                s.getEstudiante(), periodo, modalidad, TipoOportunidad.FINAL,
                ResultadoOportunidad.REPROBADO, (BigDecimal) null, java.time.LocalDate.now(),
                "Reprobado automáticamente: venció el plazo de justificación (" + s.getTipo() + ") sin resolución");
    }

    private TipoJustificacion parseTipo(String tipo) {
        try {
            return TipoJustificacion.valueOf(tipo.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de justificación inválido: " + tipo);
        }
    }

    private TipoDocumentoJustificacion parseTipoDocumentoOpcional(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) return null;
        try {
            return TipoDocumentoJustificacion.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento inválido: " + tipoDocumento);
        }
    }

    private SolicitudJustificacionDto toDto(SolicitudJustificacion s) {
        Estudiante est = s.getEstudiante();
        Usuario resuelve = s.getResueltaPor();
        return new SolicitudJustificacionDto(
                s.getId(),
                est.getIdEstudiante(),
                est.getUsuario() != null ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null,
                s.getTipo().name(),
                s.getMotivo(),
                s.getDocumentoAdjuntoId(),
                s.getTipoDocumento() != null ? s.getTipoDocumento().name() : null,
                s.getFechaEvento(),
                s.getFechaSolicitud(),
                s.getFechaLimite(),
                s.getEstado().name(),
                resuelve != null ? resuelve.getIdUsuario() : null,
                resuelve != null ? resuelve.getNombres() + " " + resuelve.getApellidos() : null,
                s.getFechaResolucion(),
                s.getNuevaFechaProgramada(),
                s.getObservacionesResolucion()
        );
    }
}
