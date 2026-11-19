package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.ExpedienteDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumentoExpediente;
import com.erwin.backend.enums.EstadoExpediente;
import com.erwin.backend.enums.EstadoExamenComplexivo;
import com.erwin.backend.enums.ModalidadCatalogoDocumento;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Módulo 15 — expediente digital del estudiante (TAREA 1). */
@Service
@Transactional
public class ExpedienteService {

    private final ExpedienteEstudianteRepository expedienteRepo;
    private final DocumentoExpedienteRepository documentoRepo;
    private final CatalogoDocumentoHabilitanteRepository catalogoRepo;
    private final EstudianteRepository estudianteRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final SustentacionRepository sustentacionRepo;
    private final ExamenComplexivoAplicacionRepository examenRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final AuthorizationService authorizationService;

    public ExpedienteService(ExpedienteEstudianteRepository expedienteRepo,
                              DocumentoExpedienteRepository documentoRepo,
                              CatalogoDocumentoHabilitanteRepository catalogoRepo,
                              EstudianteRepository estudianteRepo,
                              UsuarioRepository usuarioRepo,
                              ProyectoTitulacionRepository proyectoRepo,
                              SustentacionRepository sustentacionRepo,
                              ExamenComplexivoAplicacionRepository examenRepo,
                              PeriodoTitulacionRepository periodoRepo,
                              AuthorizationService authorizationService) {
        this.expedienteRepo = expedienteRepo;
        this.documentoRepo = documentoRepo;
        this.catalogoRepo = catalogoRepo;
        this.estudianteRepo = estudianteRepo;
        this.usuarioRepo = usuarioRepo;
        this.proyectoRepo = proyectoRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.examenRepo = examenRepo;
        this.periodoRepo = periodoRepo;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Crear expediente (TAREA 1.3)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExpedienteEstudiante", accion = "CREATE")
    public ExpedienteDto crear(CrearExpedienteRequest req) {
        authorizationService.exigirPropioEstudianteSiAplica(req.idEstudiante());
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));

        ModalidadCatalogoDocumento modalidad;
        try {
            modalidad = ModalidadCatalogoDocumento.valueOf(req.modalidad().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad inválida: " + req.modalidad());
        }
        if (modalidad == ModalidadCatalogoDocumento.AMBOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El expediente de un estudiante debe ser TIC o COMPLEXIVO, no AMBOS");
        }

        ExpedienteEstudiante expediente = new ExpedienteEstudiante();
        expediente.setEstudiante(estudiante);
        expediente.setModalidad(modalidad);
        expediente.setPeriodoTitulacion(req.periodoTitulacion());
        expediente.setEstado(EstadoExpediente.INCOMPLETO);
        expediente = expedienteRepo.save(expediente);

        Integer idFacultad = estudiante.getCarrera() != null && estudiante.getCarrera().getFacultad() != null
                ? estudiante.getCarrera().getFacultad().getIdFacultad() : null;

        List<CatalogoDocumentoHabilitante> catalogo = catalogoRepo
                .findByModalidadInOrderByOrdenAsc(List.of(modalidad, ModalidadCatalogoDocumento.AMBOS))
                .stream()
                .filter(c -> c.getFacultad() == null
                        || (idFacultad != null && c.getFacultad().getIdFacultad().equals(idFacultad)))
                .toList();

        for (CatalogoDocumentoHabilitante c : catalogo) {
            DocumentoExpediente d = new DocumentoExpediente();
            d.setExpediente(expediente);
            d.setCatalogoDocumento(c);
            d.setEstado(EstadoDocumentoExpediente.PENDIENTE);
            documentoRepo.save(d);
        }

        return obtenerPorId(expediente.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // Consultas
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ExpedienteDto obtenerPorEstudiante(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        ExpedienteEstudiante expediente = expedienteRepo
                .findFirstByEstudiante_IdEstudianteOrderByIdDesc(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EXPEDIENTE_NO_ENCONTRADO"));
        return toDto(expediente);
    }

    @Transactional(readOnly = true)
    public ExpedienteDto obtenerPorId(Long idExpediente) {
        return toDto(getExpediente(idExpediente));
    }

    /** TAREA 3.2: panel de Secretaría-Abogacía — lista filtrable de expedientes. */
    @Transactional(readOnly = true)
    public List<ExpedienteDto> listar(Integer idCarrera, String periodo, String estado) {
        return expedienteRepo.findAll().stream()
                .filter(e -> idCarrera == null || (e.getEstudiante().getCarrera() != null
                        && idCarrera.equals(e.getEstudiante().getCarrera().getIdCarrera())))
                .filter(e -> periodo == null || periodo.isBlank() || periodo.equals(e.getPeriodoTitulacion()))
                .filter(e -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(e.getEstado().name()))
                .map(this::toDto)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // Cargar / validar / rechazar documentos (TAREA 1.3)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "DocumentoExpediente", accion = "CARGAR")
    public DocumentoExpedienteDto cargar(Long idDocumentoExpediente, CargarDocumentoRequest req) {
        DocumentoExpediente doc = getDocumento(idDocumentoExpediente);
        if (req.archivoUrl() == null || req.archivoUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
        }
        doc.setArchivoUrl(req.archivoUrl());
        doc.setObservaciones(req.observaciones());
        doc.setEstado(EstadoDocumentoExpediente.CARGADO);
        doc.setMotivoRechazo(null);
        documentoRepo.save(doc);
        recalcularEstadoExpediente(doc.getExpediente());
        return toDocumentoDto(doc);
    }

    @Auditable(entidad = "DocumentoExpediente", accion = "VALIDAR")
    public DocumentoExpedienteDto validar(Long idDocumentoExpediente, ValidarDocumentoRequest req) {
        DocumentoExpediente doc = getDocumento(idDocumentoExpediente);
        if (doc.getEstado() != EstadoDocumentoExpediente.CARGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede validar un documento en estado CARGADO (actual: " + doc.getEstado() + ")");
        }
        Usuario validador = usuarioRepo.findById(req.idUsuarioValida())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        doc.setEstado(EstadoDocumentoExpediente.VALIDADO);
        doc.setValidadoPor(validador);
        doc.setFechaValidacion(LocalDateTime.now());
        documentoRepo.save(doc);
        recalcularEstadoExpediente(doc.getExpediente());
        return toDocumentoDto(doc);
    }

    @Auditable(entidad = "DocumentoExpediente", accion = "RECHAZAR")
    public DocumentoExpedienteDto rechazar(Long idDocumentoExpediente, RechazarDocumentoRequest req) {
        DocumentoExpediente doc = getDocumento(idDocumentoExpediente);
        if (doc.getEstado() != EstadoDocumentoExpediente.CARGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede rechazar un documento en estado CARGADO (actual: " + doc.getEstado() + ")");
        }
        if (req.motivoRechazo() == null || req.motivoRechazo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de rechazo es obligatorio");
        }
        Usuario validador = usuarioRepo.findById(req.idUsuarioValida())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        doc.setEstado(EstadoDocumentoExpediente.RECHAZADO);
        doc.setValidadoPor(validador);
        doc.setFechaValidacion(LocalDateTime.now());
        doc.setMotivoRechazo(req.motivoRechazo());
        documentoRepo.save(doc);
        recalcularEstadoExpediente(doc.getExpediente());
        return toDocumentoDto(doc);
    }

    private void recalcularEstadoExpediente(ExpedienteEstudiante expediente) {
        List<DocumentoExpediente> documentos = documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(expediente.getId());
        boolean todosObligatoriosCompletos = documentos.stream()
                .filter(d -> Boolean.TRUE.equals(d.getCatalogoDocumento().getObligatorio()))
                .allMatch(d -> d.getEstado() == EstadoDocumentoExpediente.VALIDADO || d.getEstado() == EstadoDocumentoExpediente.APROBADO);

        if (expediente.getEstado() == EstadoExpediente.LEGALIZADO) return; // estado final, no se revierte automáticamente
        expediente.setEstado(todosObligatoriosCompletos ? EstadoExpediente.COMPLETO : EstadoExpediente.INCOMPLETO);
        expedienteRepo.save(expediente);
    }

    /**
     * Módulo 16: registra en el expediente (si existe) el documento generado
     * automáticamente por GeneradorDocumentosService — REGLAS: "los
     * documentos generados se guardan automáticamente en el expediente".
     * No falla si el estudiante todavía no tiene expediente creado.
     */
    @Auditable(entidad = "DocumentoExpediente", accion = "REGISTRAR_GENERADO")
    public void registrarDocumentoGenerado(Integer idEstudiante, String codigoCatalogo, String archivoUrl) {
        expedienteRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(idEstudiante).ifPresent(expediente ->
                documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(expediente.getId()).stream()
                        .filter(d -> codigoCatalogo.equals(d.getCatalogoDocumento().getCodigo()))
                        .findFirst()
                        .ifPresent(d -> {
                            d.setArchivoUrl(archivoUrl);
                            d.setEstado(EstadoDocumentoExpediente.CARGADO);
                            d.setMotivoRechazo(null);
                            documentoRepo.save(d);
                            recalcularEstadoExpediente(expediente);
                        }));
    }

    // ═══════════════════════════════════════════════════════════════
    // Progreso y alertas (TAREA 1.3 / 1.4)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ProgresoExpedienteDto progreso(Long idExpediente) {
        getExpediente(idExpediente);
        List<DocumentoExpediente> documentos = documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(idExpediente);
        int total = documentos.size();
        int pendientes = contar(documentos, EstadoDocumentoExpediente.PENDIENTE);
        int cargados = contar(documentos, EstadoDocumentoExpediente.CARGADO);
        int validados = contar(documentos, EstadoDocumentoExpediente.VALIDADO);
        int aprobados = contar(documentos, EstadoDocumentoExpediente.APROBADO);
        int rechazados = contar(documentos, EstadoDocumentoExpediente.RECHAZADO);
        int faltantes = (int) documentos.stream()
                .filter(d -> Boolean.TRUE.equals(d.getCatalogoDocumento().getObligatorio()))
                .filter(d -> d.getEstado() == EstadoDocumentoExpediente.PENDIENTE || d.getEstado() == EstadoDocumentoExpediente.RECHAZADO)
                .count();

        return new ProgresoExpedienteDto(idExpediente, total, pendientes, cargados, validados, aprobados, rechazados, faltantes);
    }

    private int contar(List<DocumentoExpediente> docs, EstadoDocumentoExpediente estado) {
        return (int) docs.stream().filter(d -> d.getEstado() == estado).count();
    }

    @Transactional(readOnly = true)
    public List<AlertaExpedienteDto> alertas(Long idExpediente) {
        ExpedienteEstudiante expediente = getExpediente(idExpediente);
        LocalDate fechaEvento = resolverFechaEvento(expediente);

        return documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(idExpediente).stream()
                .filter(d -> d.getEstado() == EstadoDocumentoExpediente.PENDIENTE || d.getEstado() == EstadoDocumentoExpediente.RECHAZADO)
                .map(d -> new AlertaExpedienteDto(
                        expediente.getId(),
                        expediente.getEstudiante().getIdEstudiante(),
                        nombreCompleto(expediente.getEstudiante().getUsuario()),
                        d.getId(),
                        d.getCatalogoDocumento().getCodigo(),
                        d.getCatalogoDocumento().getNombre(),
                        d.getCatalogoDocumento().getResponsable().name(),
                        fechaEvento,
                        fechaEvento != null ? ChronoUnit.DAYS.between(LocalDate.now(), fechaEvento) : -1
                ))
                .collect(Collectors.toList());
    }

    /** Resuelve la fecha de sustentación (TIC) o de examen complexivo próxima, si existe. */
    private LocalDate resolverFechaEvento(ExpedienteEstudiante expediente) {
        Integer idEstudiante = expediente.getEstudiante().getIdEstudiante();
        if (expediente.getModalidad() == ModalidadCatalogoDocumento.TIC) {
            return proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(idEstudiante)
                    .flatMap(p -> sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(p.getIdProyecto())
                            .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                            .findFirst())
                    .map(Sustentacion::getFecha)
                    .orElse(null);
        }
        Optional<PeriodoTitulacion> periodo = periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc();
        if (periodo.isEmpty()) return null;
        return examenRepo.findByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(idEstudiante, periodo.get().getIdPeriodo())
                .stream()
                .filter(e -> e.getEstado() == EstadoExamenComplexivo.PROGRAMADO || e.getEstado() == EstadoExamenComplexivo.EN_CURSO)
                .map(ExamenComplexivoAplicacion::getFechaProgramada)
                .findFirst().orElse(null);
    }

    /** Usado por el scheduler diario — expedientes con documentos pendientes y evento a &lt;= N días. */
    @Transactional(readOnly = true)
    public List<AlertaExpedienteDto> alertasProximasSistema(int diasUmbral) {
        return expedienteRepo.findAll().stream()
                .filter(e -> e.getEstado() != EstadoExpediente.LEGALIZADO)
                .flatMap(e -> alertas(e.getId()).stream())
                .filter(a -> a.diasRestantes() >= 0 && a.diasRestantes() <= diasUmbral)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    private ExpedienteEstudiante getExpediente(Long id) {
        ExpedienteEstudiante expediente = expedienteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EXPEDIENTE_NO_ENCONTRADO"));
        if (expediente.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(expediente.getEstudiante().getIdEstudiante());
        }
        return expediente;
    }

    private DocumentoExpediente getDocumento(Long id) {
        DocumentoExpediente documento = documentoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_EXPEDIENTE_NO_ENCONTRADO"));
        if (documento.getExpediente() != null) {
            getExpediente(documento.getExpediente().getId());
        }
        return documento;
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private ExpedienteDto toDto(ExpedienteEstudiante e) {
        List<DocumentoExpedienteDto> docs = documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(e.getId())
                .stream().map(this::toDocumentoDto).collect(Collectors.toList());
        return new ExpedienteDto(
                e.getId(),
                e.getEstudiante().getIdEstudiante(),
                nombreCompleto(e.getEstudiante().getUsuario()),
                e.getModalidad().name(),
                e.getPeriodoTitulacion(),
                e.getEstado().name(),
                e.getFechaCreacion(),
                docs
        );
    }

    private DocumentoExpedienteDto toDocumentoDto(DocumentoExpediente d) {
        CatalogoDocumentoHabilitante c = d.getCatalogoDocumento();
        return new DocumentoExpedienteDto(
                d.getId(),
                c.getCodigo(),
                c.getNombre(),
                c.getDescripcion(),
                c.getModalidad().name(),
                c.getOrden(),
                c.getObligatorio(),
                c.getResponsable().name(),
                d.getEstado().name(),
                d.getArchivoUrl(),
                d.getObservaciones(),
                d.getValidadoPor() != null ? nombreCompleto(d.getValidadoPor()) : null,
                d.getFechaValidacion(),
                d.getMotivoRechazo()
        );
    }
}
