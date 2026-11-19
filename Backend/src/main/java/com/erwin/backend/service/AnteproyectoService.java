package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoAnteproyecto;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import com.erwin.backend.util.NivelAcademicoUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnteproyectoService {

    private static final int SEMANA_LIMITE_SOLICITUD_MODIFICACION = 2;

    private final AnteproyectoVersionRepository verRepo;
    private final PropuestaRepository propRepo;
    private final AnteproyectoTitulacionRepository anteRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final DocenteRepository docenteRepo;
    private final AuthorizationService authorizationService;

    public AnteproyectoService(AnteproyectoVersionRepository verRepo,
                               PropuestaRepository propRepo,
                               AnteproyectoTitulacionRepository anteRepo,
                               ProyectoTitulacionRepository proyectoRepo,
                               TribunalProyectoRepository tribunalRepo,
                               DocenteRepository docenteRepo,
                               AuthorizationService authorizationService) {
        this.verRepo = verRepo;
        this.propRepo = propRepo;
        this.anteRepo = anteRepo;
        this.proyectoRepo = proyectoRepo;
        this.tribunalRepo = tribunalRepo;
        this.docenteRepo = docenteRepo;
        this.authorizationService = authorizationService;
    }

    private AnteproyectoTitulacion getAnteproyecto(Integer idAnteproyecto) {
        AnteproyectoTitulacion a = anteRepo.findById(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ANTEPROYECTO_NO_EXISTE"));
        if (a.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(a.getEstudiante().getIdEstudiante());
        }
        return a;
    }

    /**
     * Equivalente a fn_mi_anteproyecto(idEstudiante)
     * - Busca propuesta aprobada
     * - Busca o crea anteproyecto
     * - Retorna snapshot de propuesta + ultimaVersion (si existe) o plantilla
     */
    @Transactional
    public AnteproyectoResponse cargarMiAnteproyecto(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);

        // Bug 4: la propuesta solo se marca "APROBADA" (nunca "APROBADO") — ver
        // PropuestaTitulacion.estado / ComisionTemasController.decidirPropuesta.
        PropuestaTitulacion p = propRepo
                .findFirstByEstudiante_IdEstudianteAndEstadoInOrderByIdPropuestaAsc(
                        idEstudiante,
                        List.of("APROBADA")
                )
                .orElse(null);

        AnteproyectoResponse resp = new AnteproyectoResponse();

        if (p == null) {
            resp.setEstado("NO_DISPONIBLE");
            resp.setIdEstudiante(idEstudiante);
            resp.setMensaje("Aún no tienes una propuesta APROBADA. Cuando esté aprobada podrás editar el anteproyecto.");
            return resp;
        }

        // Buscar o crear anteproyecto
        AnteproyectoTitulacion a = anteRepo.findByPropuesta_IdPropuesta(p.getIdPropuesta())
                .orElseGet(() -> {
                    AnteproyectoTitulacion nuevo = new AnteproyectoTitulacion();
                    nuevo.setPropuesta(p);
                    nuevo.setEleccion(p.getEleccion());
                    nuevo.setEstudiante(p.getEstudiante());
                    nuevo.setCarrera(p.getCarrera());
                    nuevo.setEstado(EstadoAnteproyecto.BORRADOR);
                    return anteRepo.save(nuevo);
                });

        resp.setIdAnteproyecto(a.getIdAnteproyecto());
        resp.setEstado(a.getEstado().name());
        resp.setIdEstudiante(a.getEstudiante().getIdEstudiante());

        // Nombre del estudiante (si la relación existe)
        try {
            Usuario u = a.getEstudiante().getUsuario();
            if (u != null) {
                resp.setNombresEstudiante(n(u.getNombres()));
                resp.setApellidosEstudiante(n(u.getApellidos()));
            }
        } catch (Exception ignored) {}

        // Snapshot de propuesta
        AnteproyectoResponse.PropuestaSnapshot snap = new AnteproyectoResponse.PropuestaSnapshot();
        snap.idPropuesta = p.getIdPropuesta();

        if (p.getTema() != null) {
            snap.idTema = p.getTema().getIdTema();
            snap.tituloTema = n(p.getTema().getTitulo());
        }

        // OJO: aquí usa los getters reales de tu entidad (yo dejé los de tu ejemplo)
        snap.titulo = n(p.getTitulo());

        snap.planteamientoProblema = n(p.getPlanteamientoProblema());
        snap.objetivoGeneral = n(p.getObjetivosGenerales());
        snap.objetivosEspecificos = n(p.getObjetivosEspecificos());
        snap.metodologia = n(p.getMetodologia());
        snap.bibliografia = n(p.getBibliografia());

        resp.setPropuesta(snap);

        // Última versión (equivalente a fn_ultima_version)
        var lastOpt = verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(a.getIdAnteproyecto());

        if (lastOpt.isPresent()) {
            resp.setUltimaVersion(toDto(lastOpt.get()));
            return resp;
        }

        // Plantilla si no hay versiones
        AnteproyectoVersionResponse plantilla = new AnteproyectoVersionResponse();
        plantilla.setNumeroVersion(0);
        plantilla.setEstadoVersion("PLANTILLA");
        plantilla.setTitulo(n(snap.titulo));
        plantilla.setTemaInvestigacion(n(snap.temaInvestigacion));
        plantilla.setPlanteamientoProblema(n(snap.planteamientoProblema));
        plantilla.setObjetivosGenerales(n(snap.objetivoGeneral));
        plantilla.setObjetivosEspecificos(n(snap.objetivosEspecificos));
        plantilla.setMarcoTeorico("");
        plantilla.setMetodologia(n(snap.metodologia));
        plantilla.setResultadosEsperados("");
        plantilla.setBibliografia(n(snap.bibliografia));

        resp.setUltimaVersion(plantilla);
        resp.setMensaje("Se precargaron campos desde tu propuesta aprobada. Completa y guarda el anteproyecto.");
        return resp;
    }

    public List<AnteproyectoVersionResponse> versiones(Integer idAnteproyecto) {
        getAnteproyecto(idAnteproyecto);
        return verRepo.findByAnteproyecto_IdAnteproyectoOrderByNumeroVersionAsc(idAnteproyecto)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public AnteproyectoVersionResponse ultimaVersion(Integer idAnteproyecto) {
        getAnteproyecto(idAnteproyecto);
        Anteproyectotitulacionversion v = verRepo
                .findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(idAnteproyecto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIN_VERSIONES"));
        return toDto(v);
    }

    /**
     * Equivalente a sp_guardar_borrador(...)
     * - valida bloqueado
     * - crea nueva version BORRADOR
     * - actualiza estado anteproyecto BORRADOR
     */
    @Transactional
    public AnteproyectoVersionResponse guardarBorrador(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        AnteproyectoTitulacion a = getAnteproyecto(idAnteproyecto);

        if (estaBloqueado(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ANTEPROYECTO_BLOQUEADO");
        }

        Anteproyectotitulacionversion v = crearVersion(a, req);
        v.setEstadoVersion("BORRADOR");

        // estado anteproyecto
        a.setEstado(EstadoAnteproyecto.BORRADOR);
        anteRepo.save(a);

        return toDto(verRepo.save(v));
    }

    /**
     * Equivalente a sp_enviar_revision(...)
     * - valida bloqueado
     * - crea nueva version ENVIADO
     * - actualiza estado anteproyecto EN_REVISION
     */
    @Transactional
    public AnteproyectoVersionResponse enviarRevision(Integer idAnteproyecto, AnteproyectoVersionRequest req) {

        AnteproyectoTitulacion a = getAnteproyecto(idAnteproyecto);

        if (estaBloqueado(a.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ANTEPROYECTO_BLOQUEADO");
        }

        Anteproyectotitulacionversion v = crearVersion(a, req);
        v.setEstadoVersion("ENVIADO");
        Anteproyectotitulacionversion guardada = verRepo.save(v);

        a.setEstado(EstadoAnteproyecto.EN_REVISION);
        anteRepo.save(a);

        return toDto(guardada);
    }

    // ===================== flujo de modificaciones post-aprobación =====================

    /**
     * Excepción al bloqueo post-aprobación (Módulo 6): el Director del TIC o
     * un miembro del Tribunal puede solicitar la modificación de un
     * anteproyecto ya APROBADO, hasta la semana 02 del último nivel de la
     * carrera. Requiere resolución del Consejo Directivo para desbloquearse
     * (ver {@link #aprobarModificacion}).
     */
    @Auditable(entidad = "AnteproyectoTitulacion", accion = "SOLICITAR_MODIFICACION", capturarArgs = true)
    @Transactional
    public AnteproyectoModificacionResponse solicitarModificacion(Integer idPropuesta, SolicitarModificacionRequest req) {
        AnteproyectoTitulacion a = anteRepo.findByPropuesta_IdPropuesta(idPropuesta)
                .orElseThrow(() -> new EntityNotFoundException("No existe anteproyecto para la propuesta " + idPropuesta));

        if (a.getEstado() != EstadoAnteproyecto.APROBADO) {
            throw new IllegalStateException("Solo se puede solicitar una modificación sobre un anteproyecto ya APROBADO");
        }
        if (req == null || req.getIdDocenteSolicitante() == null
                || req.getMotivoModificacion() == null || req.getMotivoModificacion().isBlank()) {
            throw new IllegalArgumentException("idDocenteSolicitante y motivoModificacion son obligatorios");
        }

        Estudiante estudiante = a.getEstudiante();
        Carrera carrera = a.getCarrera();
        int ultimo = NivelAcademicoUtil.ultimo(carrera);
        if (estudiante.getNivelSemestre() == null || estudiante.getNivelSemestre() != ultimo) {
            throw new IllegalStateException(
                    "La solicitud de modificación solo aplica a estudiantes en el último nivel (" + ultimo + ") de la carrera");
        }

        PeriodoTitulacion periodo = a.getEleccion() != null ? a.getEleccion().getPeriodo() : null;
        if (periodo != null) {
            int semana = CalendarioAcademicoUtil.semanaActual(periodo.getFechaInicio(), LocalDate.now());
            if (semana > SEMANA_LIMITE_SOLICITUD_MODIFICACION) {
                throw new IllegalStateException(
                        "La solicitud de modificación solo está permitida hasta la semana "
                                + SEMANA_LIMITE_SOLICITUD_MODIFICACION + " del último nivel (semana actual: " + semana + ")");
            }
        }

        ProyectoTitulacion proyecto = proyectoRepo.findByPropuesta_IdPropuesta(idPropuesta).orElse(null);
        boolean esDirector = proyecto != null && proyecto.getDirector() != null
                && proyecto.getDirector().getIdDocente().equals(req.getIdDocenteSolicitante());
        boolean esTribunal = proyecto != null && tribunalRepo
                .existsByProyecto_IdProyectoAndDocente_IdDocente(proyecto.getIdProyecto(), req.getIdDocenteSolicitante());
        if (!esDirector && !esTribunal) {
            throw new AccessDeniedException("Solo el Director del TIC o un miembro del Tribunal puede solicitar la modificación");
        }

        Docente solicitante = docenteRepo.findById(req.getIdDocenteSolicitante())
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + req.getIdDocenteSolicitante()));

        a.setMotivoModificacionSolicitada(req.getMotivoModificacion().trim());
        a.setCamposAModificarSolicitados(
                req.getCamposAModificar() == null ? null : String.join(", ", req.getCamposAModificar()));
        a.setSolicitadoPor(solicitante);
        a.setEstado(EstadoAnteproyecto.MODIFICACION_SOLICITADA);
        anteRepo.save(a);

        return new AnteproyectoModificacionResponse(
                a.getIdAnteproyecto(), a.getEstado().name(),
                a.getMotivoModificacionSolicitada(), a.getCamposAModificarSolicitados(),
                "Solicitud de modificación registrada; pendiente de resolución del Consejo Directivo");
    }

    /**
     * El Consejo Directivo (COORDINADOR/DECANO) aprueba la solicitud:
     * desbloquea temporalmente el anteproyecto (vuelve a BORRADOR, el único
     * estado no bloqueado por {@link #estaBloqueado}) y dispara la creación
     * de una nueva versión que preserva el historial.
     */
    @Auditable(entidad = "AnteproyectoTitulacion", accion = "APROBAR_MODIFICACION")
    @Transactional
    public AnteproyectoVersionResponse aprobarModificacion(Integer idPropuesta, AprobarModificacionRequest req) {
        AnteproyectoTitulacion a = anteRepo.findByPropuesta_IdPropuesta(idPropuesta)
                .orElseThrow(() -> new EntityNotFoundException("No existe anteproyecto para la propuesta " + idPropuesta));

        if (a.getEstado() != EstadoAnteproyecto.MODIFICACION_SOLICITADA) {
            throw new IllegalStateException("No hay una solicitud de modificación pendiente para este anteproyecto");
        }

        Anteproyectotitulacionversion nueva = new Anteproyectotitulacionversion();
        nueva.setAnteproyecto(a);
        nueva.setNumeroVersion(verRepo.maxNumeroVersion(a.getIdAnteproyecto()) + 1);
        nueva.setEstadoVersion("MODIFICACION_APROBADA");

        verRepo.findTopByAnteproyecto_IdAnteproyectoOrderByNumeroVersionDesc(a.getIdAnteproyecto())
                .ifPresent(ultima -> copiarCampos(ultima, nueva));

        String motivo = n(a.getMotivoModificacionSolicitada());
        nueva.setComentarioCambio(("Modificación aprobada por Consejo Directivo: " + motivo)
                .substring(0, Math.min(300, ("Modificación aprobada por Consejo Directivo: " + motivo).length())));

        Anteproyectotitulacionversion guardada = verRepo.save(nueva);

        a.setEstado(EstadoAnteproyecto.BORRADOR);
        anteRepo.save(a);

        return toDto(guardada);
    }

    private void copiarCampos(Anteproyectotitulacionversion origen, Anteproyectotitulacionversion destino) {
        destino.setTitulo(n(origen.getTitulo()));
        destino.setTemaInvestigacion(n(origen.getTemaInvestigacion()));
        destino.setPlanteamientoProblema(n(origen.getPlanteamientoProblema()));
        destino.setObjetivosGenerales(n(origen.getObjetivosGenerales()));
        destino.setObjetivosEspecificos(n(origen.getObjetivosEspecificos()));
        destino.setMarcoTeorico(n(origen.getMarcoTeorico()));
        destino.setMetodologia(n(origen.getMetodologia()));
        destino.setResultadosEsperados(n(origen.getResultadosEsperados()));
        destino.setBibliografia(n(origen.getBibliografia()));
    }

    // ===================== helpers =====================

    private boolean estaBloqueado(EstadoAnteproyecto estado) {
        if (estado == null) return false;
        return estado == EstadoAnteproyecto.APROBADO
                || estado == EstadoAnteproyecto.RECHAZADO
                || estado == EstadoAnteproyecto.EN_REVISION;
    }

    private Anteproyectotitulacionversion crearVersion(AnteproyectoTitulacion a, AnteproyectoVersionRequest req) {
        int next = verRepo.maxNumeroVersion(a.getIdAnteproyecto()) + 1;

        Anteproyectotitulacionversion v = new Anteproyectotitulacionversion();
        v.setAnteproyecto(a);
        v.setNumeroVersion(next);

        v.setTitulo(n(req.titulo));
        v.setTemaInvestigacion(n(req.temaInvestigacion));
        v.setPlanteamientoProblema(n(req.planteamientoProblema));
        v.setObjetivosGenerales(n(req.objetivosGenerales));
        v.setObjetivosEspecificos(n(req.objetivosEspecificos));
        v.setMarcoTeorico(n(req.marcoTeorico));
        v.setMetodologia(n(req.metodologia));
        v.setResultadosEsperados(n(req.resultadosEsperados));
        v.setBibliografia(n(req.bibliografia));
        v.setComentarioCambio(n(req.comentarioCambio));

        // si manejas fecha en entidad:
        // v.setFechaEnvio(LocalDate.now());

        return v;
    }

    private AnteproyectoVersionResponse toDto(Anteproyectotitulacionversion v) {
        AnteproyectoVersionResponse r = new AnteproyectoVersionResponse();
        r.setIdVersion(v.getIdVersion());
        r.setNumeroVersion(v.getNumeroVersion());
        r.setEstadoVersion(v.getEstadoVersion());
        r.setFechaCreacion(v.getFechaEnvio());   // ajusta si tu campo se llama distinto
        r.setComentarioCambio(v.getComentarioCambio());

        r.setTitulo(v.getTitulo());
        r.setTemaInvestigacion(v.getTemaInvestigacion());
        r.setPlanteamientoProblema(v.getPlanteamientoProblema());
        r.setObjetivosGenerales(v.getObjetivosGenerales());
        r.setObjetivosEspecificos(v.getObjetivosEspecificos());
        r.setMarcoTeorico(v.getMarcoTeorico());
        r.setMetodologia(v.getMetodologia());
        r.setResultadosEsperados(v.getResultadosEsperados());
        r.setBibliografia(v.getBibliografia());
        return r;
    }

    private String n(String s) { return (s == null) ? "" : s; }
}