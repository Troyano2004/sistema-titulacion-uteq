package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.DocumentoTicDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoSeccionTic;
import com.erwin.backend.enums.ModalidadEntrega;
import com.erwin.backend.enums.RolAutorComentarioTic;
import com.erwin.backend.enums.TipoTicPlantilla;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Módulo del editor por secciones del TIC (TAREA 1). Modelo nuevo y
 * paralelo a {@link DocumentoTitulacion} — no lo toca ni lo reemplaza.
 */
@Service
@Transactional
public class DocumentoTicService {

    private static final List<String> CODIGOS_AUTOLLENADOS = List.of("PORTADA", "CERTIFICACION_DIRECTOR", "INDICE", "CODIGO_DUBLIN");

    private final DocumentoTicRepository documentoRepo;
    private final SeccionDocumentoTicRepository seccionRepo;
    private final VersionDocumentoTicRepository versionRepo;
    private final ComentarioDocumentoTicRepository comentarioRepo;
    private final EstudianteRepository estudianteRepo;
    private final TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    private final DocenteRepository docenteRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuthorizationService authorizationService;
    private final SeccionPlantillaTicService seccionPlantillaTicService;

    public DocumentoTicService(DocumentoTicRepository documentoRepo,
                                SeccionDocumentoTicRepository seccionRepo,
                                VersionDocumentoTicRepository versionRepo,
                                ComentarioDocumentoTicRepository comentarioRepo,
                                EstudianteRepository estudianteRepo,
                                TipoTrabajoTitulacionRepository tipoTrabajoRepo,
                                DocenteRepository docenteRepo,
                                UsuarioRepository usuarioRepo,
                                AuthorizationService authorizationService,
                                SeccionPlantillaTicService seccionPlantillaTicService) {
        this.documentoRepo = documentoRepo;
        this.seccionRepo = seccionRepo;
        this.versionRepo = versionRepo;
        this.comentarioRepo = comentarioRepo;
        this.estudianteRepo = estudianteRepo;
        this.tipoTrabajoRepo = tipoTrabajoRepo;
        this.docenteRepo = docenteRepo;
        this.usuarioRepo = usuarioRepo;
        this.authorizationService = authorizationService;
        this.seccionPlantillaTicService = seccionPlantillaTicService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Crear (TAREA 1.2 — genera secciones según el tipo)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "DocumentoTIC", accion = "CREATE")
    public DocumentoTicResponse crear(CrearDocumentoTicRequest req) {
        authorizationService.exigirPropioEstudianteSiAplica(req.idEstudiante());
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));
        Tipotrabajotitulacion tipoTrabajo = tipoTrabajoRepo.findById(req.idTipoTrabajo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TIPO_TRABAJO_NO_ENCONTRADO"));

        ModalidadEntrega modalidad;
        try {
            modalidad = ModalidadEntrega.valueOf(req.modalidadEntrega().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad de entrega inválida: " + req.modalidadEntrega());
        }

        DocumentoTIC documento = new DocumentoTIC();
        documento.setEstudiante(estudiante);
        documento.setTipoTrabajo(tipoTrabajo);
        documento.setModalidadEntrega(modalidad);
        documento.setTitulo(req.titulo());
        if (req.idDirector() != null) {
            documento.setDirector(docenteRepo.findById(req.idDirector())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO")));
        }
        documento = documentoRepo.save(documento);

        if (modalidad == ModalidadEntrega.REDACCION_SISTEMA) {
            TipoTicPlantilla plantilla = SeccionPlantillaProvider.clasificar(tipoTrabajo.getNombre());
            int orden = 1;
            for (SeccionPlantillaProvider.SeccionDef def : seccionPlantillaTicService.plantillaPara(plantilla)) {
                SeccionDocumentoTIC seccion = new SeccionDocumentoTIC();
                seccion.setDocumento(documento);
                seccion.setCodigoSeccion(def.codigo());
                seccion.setTitulo(def.titulo());
                seccion.setOrden(orden++);
                seccion.setObligatoria(def.obligatoria());
                seccion.setAutoLlenada(def.autoLlenada());
                seccion.setEstado(EstadoSeccionTic.VACIO);
                seccionRepo.save(seccion);
            }
            regenerarSeccionesAutomaticas(documento);
        }

        return obtener(documento.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // Obtener
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public DocumentoTicResponse obtener(Long idDocumento) {
        DocumentoTIC documento = getDocumento(idDocumento);
        List<SeccionDocumentoTIC> secciones = seccionRepo.findByDocumento_IdOrderByOrdenAsc(idDocumento);
        return toResponse(documento, secciones);
    }

    @Transactional(readOnly = true)
    public Optional<DocumentoTicResponse> obtenerPorEstudiante(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return documentoRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(idEstudiante)
                .map(d -> toResponse(d, seccionRepo.findByDocumento_IdOrderByOrdenAsc(d.getId())));
    }

    // ═══════════════════════════════════════════════════════════════
    // Guardar sección (TAREA 1.4)
    // ═══════════════════════════════════════════════════════════════
    public SeccionTicResponse guardarSeccion(Long idDocumento, String codigoSeccion, GuardarSeccionRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);
        SeccionDocumentoTIC seccion = seccionRepo.findByDocumento_IdAndCodigoSeccion(idDocumento, codigoSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SECCION_NO_ENCONTRADA"));

        if (Boolean.TRUE.equals(seccion.getAutoLlenada())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La sección " + codigoSeccion + " se genera automáticamente y no se puede editar directamente");
        }

        seccion.setContenidoHtml(req.contenidoHtml());
        seccion.setEstado(calcularEstadoSeccion(req.contenidoHtml()));
        seccionRepo.save(seccion);

        documento.setFechaUltimaModificacion(LocalDateTime.now());
        documentoRepo.save(documento);

        if ("RESUMEN".equals(codigoSeccion)) {
            regenerarSeccionesAutomaticas(documento); // CODIGO_DUBLIN embebe el resumen
        }

        return toSeccionDto(seccion);
    }

    /** Registra la URL del PDF subido para modalidad UPLOAD_PDF (TAREA 2.5). */
    public DocumentoTicResponse registrarArchivoPdf(Long idDocumento, RegistrarArchivoPdfRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);
        if (documento.getModalidadEntrega() != ModalidadEntrega.UPLOAD_PDF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este documento no está configurado en modalidad UPLOAD_PDF");
        }
        documento.setArchivoPdfUrl(req.archivoPdfUrl());
        documento.setFechaUltimaModificacion(LocalDateTime.now());
        documentoRepo.save(documento);
        return obtener(idDocumento);
    }

    private EstadoSeccionTic calcularEstadoSeccion(String html) {
        String texto = html == null ? "" : html.replaceAll("<[^>]*>", "").trim();
        if (texto.isEmpty()) return EstadoSeccionTic.VACIO;
        return texto.length() < 50 ? EstadoSeccionTic.EN_PROGRESO : EstadoSeccionTic.COMPLETO;
    }

    // ═══════════════════════════════════════════════════════════════
    // Secciones personalizadas (ARTICULO_CIENTIFICO — TAREA 1.2, flexible)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "SeccionDocumentoTIC", accion = "CREAR_PERSONALIZADA")
    public SeccionTicResponse crearSeccionPersonalizada(Long idDocumento, CrearSeccionPersonalizadaRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);
        if (req.codigoSeccion() == null || req.codigoSeccion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de sección es obligatorio");
        }
        String codigo = req.codigoSeccion().trim().toUpperCase().replace(' ', '_');
        if (seccionRepo.findByDocumento_IdAndCodigoSeccion(idDocumento, codigo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sección con ese código");
        }

        int siguienteOrden = seccionRepo.findByDocumento_IdOrderByOrdenAsc(idDocumento).stream()
                .mapToInt(SeccionDocumentoTIC::getOrden).max().orElse(0) + 1;

        SeccionDocumentoTIC seccion = new SeccionDocumentoTIC();
        seccion.setDocumento(documento);
        seccion.setCodigoSeccion(codigo);
        seccion.setTitulo(req.titulo() != null && !req.titulo().isBlank() ? req.titulo() : codigo);
        seccion.setOrden(siguienteOrden);
        seccion.setObligatoria(Boolean.TRUE.equals(req.obligatoria()));
        seccion.setAutoLlenada(false);
        seccion.setEstado(EstadoSeccionTic.VACIO);
        return toSeccionDto(seccionRepo.save(seccion));
    }

    public void eliminarSeccionPersonalizada(Long idDocumento, String codigoSeccion) {
        SeccionDocumentoTIC seccion = seccionRepo.findByDocumento_IdAndCodigoSeccion(idDocumento, codigoSeccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SECCION_NO_ENCONTRADA"));
        if (Boolean.TRUE.equals(seccion.getObligatoria()) || Boolean.TRUE.equals(seccion.getAutoLlenada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede eliminar una sección obligatoria o autogenerada");
        }
        seccionRepo.delete(seccion);
    }

    // ═══════════════════════════════════════════════════════════════
    // Versiones (snapshot explícito — nunca automático, ver REGLAS)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "VersionDocumentoTIC", accion = "CREAR")
    public VersionTicResponse crearVersion(Long idDocumento, CrearVersionRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);
        List<SeccionDocumentoTIC> secciones = seccionRepo.findByDocumento_IdOrderByOrdenAsc(idDocumento);

        int siguiente = versionRepo.findFirstByDocumento_IdOrderByNumeroVersionDesc(idDocumento)
                .map(v -> v.getNumeroVersion() + 1).orElse(1);

        VersionDocumentoTIC version = new VersionDocumentoTIC();
        version.setDocumento(documento);
        version.setNumeroVersion(siguiente);
        version.setCreadaPor(req.creadaPor() != null && !req.creadaPor().isBlank() ? req.creadaPor() : "Sistema");
        version.setContenidoSnapshot(construirSnapshotJson(secciones));
        version = versionRepo.save(version);

        return new VersionTicResponse(version.getId(), version.getNumeroVersion(), version.getFechaVersion(), version.getCreadaPor());
    }

    @Transactional(readOnly = true)
    public List<VersionTicResponse> listarVersiones(Long idDocumento) {
        getDocumento(idDocumento);
        return versionRepo.findByDocumento_IdOrderByNumeroVersionDesc(idDocumento).stream()
                .map(v -> new VersionTicResponse(v.getId(), v.getNumeroVersion(), v.getFechaVersion(), v.getCreadaPor()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionTicDetalleResponse obtenerVersion(Long idDocumento, Long idVersion) {
        getDocumento(idDocumento);
        VersionDocumentoTIC v = versionRepo.findById(idVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VERSION_NO_ENCONTRADA"));
        if (!v.getDocumento().getId().equals(idDocumento)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VERSION_NO_ENCONTRADA");
        }
        return new VersionTicDetalleResponse(v.getId(), v.getNumeroVersion(), v.getFechaVersion(), v.getCreadaPor(), v.getContenidoSnapshot());
    }

    private String construirSnapshotJson(List<SeccionDocumentoTIC> secciones) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < secciones.size(); i++) {
            SeccionDocumentoTIC s = secciones.get(i);
            if (i > 0) json.append(',');
            json.append("{\"codigoSeccion\":\"").append(escapeJson(s.getCodigoSeccion())).append('"')
                    .append(",\"titulo\":\"").append(escapeJson(s.getTitulo())).append('"')
                    .append(",\"orden\":").append(s.getOrden())
                    .append(",\"contenidoHtml\":\"").append(escapeJson(s.getContenidoHtml())).append('"')
                    .append('}');
        }
        json.append(']');
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    // ═══════════════════════════════════════════════════════════════
    // Comentarios (director/tribunal — visibles también para el estudiante)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ComentarioDocumentoTIC", accion = "CREATE")
    public ComentarioTicResponse agregarComentario(Long idDocumento, ComentarioTicRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);
        Usuario autor = usuarioRepo.findById(req.idUsuarioAutor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        RolAutorComentarioTic rol;
        try {
            rol = RolAutorComentarioTic.valueOf(req.rolAutor().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol de autor inválido: " + req.rolAutor());
        }
        if (req.contenido() == null || req.contenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido del comentario es obligatorio");
        }

        SeccionDocumentoTIC seccion = null;
        if (req.idSeccion() != null) {
            seccion = seccionRepo.findById(req.idSeccion())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SECCION_NO_ENCONTRADA"));
        }

        ComentarioDocumentoTIC comentario = new ComentarioDocumentoTIC();
        comentario.setDocumento(documento);
        comentario.setSeccion(seccion);
        comentario.setAutor(autor);
        comentario.setRolAutor(rol);
        comentario.setContenido(req.contenido());
        return toComentarioDto(comentarioRepo.save(comentario));
    }

    @Transactional(readOnly = true)
    public List<ComentarioTicResponse> listarComentarios(Long idDocumento) {
        getDocumento(idDocumento);
        return comentarioRepo.findByDocumento_IdOrderByFechaCreacionAsc(idDocumento).stream()
                .map(this::toComentarioDto).collect(Collectors.toList());
    }

    public ComentarioTicResponse resolverComentario(Long idDocumento, Long idComentario) {
        getDocumento(idDocumento);
        ComentarioDocumentoTIC comentario = comentarioRepo.findById(idComentario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "COMENTARIO_NO_ENCONTRADO"));
        if (!comentario.getDocumento().getId().equals(idDocumento)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "COMENTARIO_NO_ENCONTRADO");
        }
        comentario.setResuelto(true);
        return toComentarioDto(comentarioRepo.save(comentario));
    }

    // ═══════════════════════════════════════════════════════════════
    // Auto-llenado (TAREA 1.3)
    // ═══════════════════════════════════════════════════════════════
    private void regenerarSeccionesAutomaticas(DocumentoTIC documento) {
        List<SeccionDocumentoTIC> secciones = seccionRepo.findByDocumento_IdOrderByOrdenAsc(documento.getId());

        Estudiante estudiante = documento.getEstudiante();
        Carrera carrera = estudiante.getCarrera();
        Facultad facultad = carrera != null ? carrera.getFacultad() : null;
        Universidad universidad = facultad != null ? facultad.getUniversidad() : null;
        Docente director = documento.getDirector();

        String nombreEstudiante = nombreCompleto(estudiante.getUsuario());
        String nombreDirector = director != null ? nombreCompleto(director.getUsuario()) : "(sin director asignado)";
        String tituloDirector = director != null ? tituloAcademico(director.getNivelPosgrado()) : "";
        int anio = LocalDateTime.now().getYear();

        for (SeccionDocumentoTIC seccion : secciones) {
            String html = switch (seccion.getCodigoSeccion()) {
                case "PORTADA" -> generarPortada(universidad, facultad, carrera, documento, nombreEstudiante, tituloDirector, nombreDirector, anio);
                case "CERTIFICACION_DIRECTOR" -> generarCertificacionDirector(tituloDirector, nombreDirector, nombreEstudiante, documento);
                case "INDICE" -> generarIndice(secciones);
                case "CODIGO_DUBLIN" -> generarCodigoDublin(documento, nombreEstudiante, secciones, anio);
                default -> null;
            };
            if (html != null) {
                seccion.setContenidoHtml(html);
                seccion.setEstado(EstadoSeccionTic.COMPLETO);
                seccionRepo.save(seccion);
            }
        }
    }

    private String tituloAcademico(com.erwin.backend.enums.NivelPosgrado nivel) {
        if (nivel == null) return "";
        return switch (nivel) {
            case PHD -> "PhD.";
            case MAGISTER -> "Mg.";
            case ESPECIALISTA -> "Esp.";
            case NINGUNO -> "";
        };
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return (n(u.getNombres()) + " " + n(u.getApellidos())).trim();
    }

    private String n(String s) { return s == null ? "" : s; }

    private String generarPortada(Universidad universidad, Facultad facultad, Carrera carrera, DocumentoTIC doc,
                                   String nombreEstudiante, String tituloDirector, String nombreDirector, int anio) {
        return "<div class=\"portada\">"
                + "<p style=\"text-align:center\">" + (universidad != null ? universidad.getNombre() : "UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO") + "</p>"
                + "<p style=\"text-align:center\">" + (facultad != null ? facultad.getNombre() : "") + "</p>"
                + "<p style=\"text-align:center\">" + (carrera != null ? carrera.getNombre() : "") + "</p>"
                + "<h2 style=\"text-align:center\">" + n(doc.getTitulo()) + "</h2>"
                + "<p style=\"text-align:center\">Trabajo de titulación previo a la obtención del título correspondiente</p>"
                + "<p style=\"text-align:center\"><strong>Autor:</strong> " + nombreEstudiante + "</p>"
                + "<p style=\"text-align:center\"><strong>Director:</strong> " + tituloDirector + " " + nombreDirector + "</p>"
                + "<p style=\"text-align:center\">" + n(doc.getCiudad()) + " - Ecuador</p>"
                + "<p style=\"text-align:center\">" + anio + "</p>"
                + "</div>";
    }

    private String generarCertificacionDirector(String tituloDirector, String nombreDirector, String nombreEstudiante, DocumentoTIC doc) {
        return "<div class=\"certificacion\">"
                + "<p>Quien suscribe, " + tituloDirector + " " + nombreDirector + ", en calidad de Director del trabajo de titulación "
                + "\"" + n(doc.getTitulo()) + "\", elaborado por el/la estudiante " + nombreEstudiante
                + ", certifica que el presente trabajo cumple con los requisitos técnicos, científicos y reglamentarios "
                + "para su presentación y defensa.</p>"
                + "<p style=\"text-align:right\">" + tituloDirector + " " + nombreDirector + "<br>DIRECTOR</p>"
                + "</div>";
    }

    private String generarIndice(List<SeccionDocumentoTIC> secciones) {
        StringBuilder html = new StringBuilder("<div class=\"indice\"><ol>");
        for (SeccionDocumentoTIC s : secciones) {
            if ("INDICE".equals(s.getCodigoSeccion())) continue;
            html.append("<li>").append(s.getTitulo()).append("</li>");
        }
        html.append("</ol></div>");
        return html.toString();
    }

    private String generarCodigoDublin(DocumentoTIC doc, String nombreEstudiante, List<SeccionDocumentoTIC> secciones, int anio) {
        String resumen = secciones.stream()
                .filter(s -> "RESUMEN".equals(s.getCodigoSeccion()))
                .map(s -> s.getContenidoHtml() != null ? s.getContenidoHtml() : "")
                .findFirst().orElse("");
        String resumenTexto = resumen == null ? "" : resumen.replaceAll("<[^>]*>", "").trim();
        if (resumenTexto.length() > 400) resumenTexto = resumenTexto.substring(0, 400) + "...";

        return "<table class=\"codigo-dublin\" style=\"width:100%; border-collapse:collapse;\">"
                + fila("Título", n(doc.getTitulo()))
                + fila("Autor", nombreEstudiante)
                + fila("Palabras clave", "(pendiente de completar)")
                + fila("Fecha", java.time.LocalDate.now().toString())
                + fila("Editorial", "Quevedo-UTEQ, " + anio)
                + fila("Resumen", resumenTexto.isBlank() ? "(pendiente — se toma de la sección Resumen)" : resumenTexto)
                + fila("Descripción", n(doc.getTitulo()))
                + fila("URI", "(se asigna al publicar en el repositorio institucional)")
                + "</table>";
    }

    private String fila(String etiqueta, String valor) {
        return "<tr><td style=\"border:1px solid #999;padding:4px;font-weight:bold;width:25%\">" + etiqueta
                + "</td><td style=\"border:1px solid #999;padding:4px\">" + valor + "</td></tr>";
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    DocumentoTIC getDocumento(Long idDocumento) {
        DocumentoTIC documento = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NO_ENCONTRADO"));
        if (documento.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(documento.getEstudiante().getIdEstudiante());
        }
        return documento;
    }

    private SeccionTicResponse toSeccionDto(SeccionDocumentoTIC s) {
        return new SeccionTicResponse(s.getId(), s.getCodigoSeccion(), s.getTitulo(), s.getContenidoHtml(),
                s.getOrden(), s.getObligatoria(), s.getAutoLlenada(), s.getEstado().name());
    }

    private ComentarioTicResponse toComentarioDto(ComentarioDocumentoTIC c) {
        return new ComentarioTicResponse(
                c.getId(),
                c.getSeccion() != null ? c.getSeccion().getId() : null,
                c.getSeccion() != null ? c.getSeccion().getCodigoSeccion() : null,
                c.getAutor().getIdUsuario(),
                nombreCompleto(c.getAutor()),
                c.getRolAutor().name(),
                c.getContenido(),
                c.getFechaCreacion(),
                c.getResuelto()
        );
    }

    private DocumentoTicResponse toResponse(DocumentoTIC d, List<SeccionDocumentoTIC> secciones) {
        List<SeccionTicResponse> seccionesDto = secciones.stream()
                .sorted(Comparator.comparing(SeccionDocumentoTIC::getOrden))
                .map(this::toSeccionDto).collect(Collectors.toList());
        int completas = (int) secciones.stream().filter(s -> s.getEstado() == EstadoSeccionTic.COMPLETO).count();

        return new DocumentoTicResponse(
                d.getId(),
                d.getEstudiante().getIdEstudiante(),
                nombreCompleto(d.getEstudiante().getUsuario()),
                d.getTipoTrabajo().getIdTipoTrabajo(),
                d.getTipoTrabajo().getNombre(),
                d.getDirector() != null ? d.getDirector().getIdDocente() : null,
                d.getDirector() != null ? nombreCompleto(d.getDirector().getUsuario()) : null,
                d.getModalidadEntrega().name(),
                d.getEstado().name(),
                d.getTitulo(),
                d.getCiudad(),
                d.getFechaCreacion(),
                d.getFechaUltimaModificacion(),
                d.getArchivoPdfUrl(),
                completas,
                secciones.size(),
                seccionesDto
        );
    }
}
