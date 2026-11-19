package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.AprobarYDesignarRequest;
import com.erwin.backend.dtos.AprobarYDesignarResponse;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.entities.PropuestaTitulacion;
import com.erwin.backend.entities.TribunalProyecto;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.NivelPosgrado;
import com.erwin.backend.enums.TipoVinculacion;
import com.erwin.backend.repository.DocenteCarreraRepository;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.erwin.backend.repository.TribunalProyectoRepository;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolución del Consejo Directivo sobre una propuesta del TIC ya aprobada
 * por la Comisión: designa Director + Tribunal (1 Presidente + 2 Vocales) y
 * los deja registrados sobre el ProyectoTitulacion que ya se generó al
 * aprobarse la propuesta (ver ComisionTemasController.decidirPropuesta).
 */
@Service
@Transactional
public class PropuestaResolucionService {

    private static final Logger log = LoggerFactory.getLogger(PropuestaResolucionService.class);
    private static final int DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO = 10;

    private final PropuestaTitulacionRepository propuestaRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final DocenteRepository docenteRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final UsuarioRepository usuarioRepo;

    public PropuestaResolucionService(PropuestaTitulacionRepository propuestaRepo,
                                       ProyectoTitulacionRepository proyectoRepo,
                                       DocenteRepository docenteRepo,
                                       DocenteCarreraRepository docenteCarreraRepo,
                                       TribunalProyectoRepository tribunalRepo,
                                       UsuarioRepository usuarioRepo) {
        this.propuestaRepo = propuestaRepo;
        this.proyectoRepo = proyectoRepo;
        this.docenteRepo = docenteRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
        this.tribunalRepo = tribunalRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Auditable(entidad = "PropuestaTitulacion", accion = "APROBAR_Y_DESIGNAR", capturarArgs = true)
    public AprobarYDesignarResponse aprobarYDesignar(Integer idPropuesta, AprobarYDesignarRequest req) {
        PropuestaTitulacion propuesta = propuestaRepo.findById(idPropuesta)
                .orElseThrow(() -> new EntityNotFoundException("Propuesta no encontrada: " + idPropuesta));

        if (!"APROBADA".equals(propuesta.getEstado())) {
            throw new IllegalStateException("La propuesta debe estar APROBADA por la Comisión antes de la resolución del Consejo Directivo");
        }
        if (Boolean.TRUE.equals(propuesta.getResolucionConsejoDirectivo())) {
            throw new IllegalStateException("Esta propuesta ya tiene una resolución del Consejo Directivo registrada");
        }
        if (req == null || req.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario (Decano/Coordinador) que resuelve");
        }
        if (req.getDirectorId() == null || req.getPresidenteTribunalId() == null
                || req.getVocal1Id() == null || req.getVocal2Id() == null) {
            throw new IllegalArgumentException("Debe indicar directorId, presidenteTribunalId, vocal1Id y vocal2Id");
        }

        Set<Integer> idsDistintos = new HashSet<>(java.util.List.of(
                req.getDirectorId(), req.getPresidenteTribunalId(), req.getVocal1Id(), req.getVocal2Id()));
        if (idsDistintos.size() != 4) {
            throw new IllegalArgumentException("El Director y los 3 miembros del Tribunal deben ser docentes distintos");
        }

        ProyectoTitulacion proyecto = proyectoRepo.findByPropuesta_IdPropuesta(idPropuesta)
                .orElseThrow(() -> new IllegalStateException("La propuesta aún no tiene un proyecto generado"));

        Integer idCarrera = propuesta.getCarrera().getIdCarrera();
        Docente director = obtenerDocenteElegible(req.getDirectorId(), idCarrera, "Director");
        Docente presidente = obtenerDocenteElegible(req.getPresidenteTribunalId(), idCarrera, "Presidente del Tribunal");
        Docente vocal1 = obtenerDocenteElegible(req.getVocal1Id(), idCarrera, "Vocal 1 del Tribunal");
        Docente vocal2 = obtenerDocenteElegible(req.getVocal2Id(), idCarrera, "Vocal 2 del Tribunal");

        Usuario usuario = usuarioRepo.findById(req.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + req.getIdUsuario()));

        proyecto.setDirector(director);
        proyectoRepo.save(proyecto);

        tribunalRepo.deleteByProyecto_IdProyecto(proyecto.getIdProyecto());
        guardarMiembroTribunal(proyecto, presidente, "PRESIDENTE");
        guardarMiembroTribunal(proyecto, vocal1, "VOCAL");
        guardarMiembroTribunal(proyecto, vocal2, "VOCAL");

        propuesta.setResolucionConsejoDirectivo(true);
        propuesta.setFechaResolucionConsejoDirectivo(LocalDateTime.now());
        propuesta.setAprobadoPorConsejo(usuario);
        propuestaRepo.save(propuesta);

        alertarSiPlazoVencido(propuesta);

        return new AprobarYDesignarResponse(
                propuesta.getIdPropuesta(),
                proyecto.getIdProyecto(),
                nombreCompleto(director),
                nombreCompleto(presidente),
                nombreCompleto(vocal1),
                nombreCompleto(vocal2),
                "Director y Tribunal designados correctamente");
    }

    @Transactional(readOnly = true)
    public List<com.erwin.backend.dtos.PropuestaPendienteResponse> listarPendientes(Integer idCarrera) {
        List<PropuestaTitulacion> pendientes = idCarrera != null
                ? propuestaRepo.findByEstadoAndResolucionConsejoDirectivoFalseAndCarrera_IdCarrera("APROBADA", idCarrera)
                : propuestaRepo.findByEstadoAndResolucionConsejoDirectivoFalse("APROBADA");

        LocalDateTime ahora = LocalDateTime.now();
        return pendientes.stream()
                .map(p -> new com.erwin.backend.dtos.PropuestaPendienteResponse(
                        p.getIdPropuesta(),
                        p.getTitulo(),
                        nombreCompletoEstudiante(p),
                        p.getCarrera() != null ? p.getCarrera().getIdCarrera() : null,
                        p.getCarrera() != null ? p.getCarrera().getNombre() : null,
                        p.getFechaRecepcionComision(),
                        p.getFechaRecepcionComision() != null
                                ? CalendarioAcademicoUtil.diasHabilesEntre(p.getFechaRecepcionComision(), ahora)
                                : 0
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.erwin.backend.dtos.DocenteElegibleResponse> listarDocentesElegibles(Integer idCarrera) {
        return docenteCarreraRepo.findByCarrera_IdCarreraAndActivoTrue(idCarrera).stream()
                .map(com.erwin.backend.entities.DocenteCarrera::getDocente)
                .filter(d -> d.getTipoVinculacion() == TipoVinculacion.PLANTA)
                .filter(d -> d.getNivelPosgrado() != null && d.getNivelPosgrado() != NivelPosgrado.NINGUNO)
                .map(d -> new com.erwin.backend.dtos.DocenteElegibleResponse(
                        d.getIdDocente(), nombreCompleto(d),
                        d.getTipoVinculacion().name(), d.getNivelPosgrado().name()))
                .toList();
    }

    private String nombreCompletoEstudiante(PropuestaTitulacion p) {
        if (p.getEstudiante() == null || p.getEstudiante().getUsuario() == null) return "";
        return (p.getEstudiante().getUsuario().getNombres() + " " + p.getEstudiante().getUsuario().getApellidos()).trim();
    }

    private void guardarMiembroTribunal(ProyectoTitulacion proyecto, Docente docente, String cargo) {
        TribunalProyecto tp = new TribunalProyecto();
        tp.setProyecto(proyecto);
        tp.setDocente(docente);
        tp.setCargo(cargo);
        tribunalRepo.save(tp);
    }

    /**
     * Elegibilidad de Director/Tribunal del TIC: docente de planta UTEQ, con
     * posgrado, y vinculado a la carrera de la propuesta (proxy de "área
     * afín" — el sistema no tiene un mapeo formal área-de-conocimiento ↔
     * carrera más allá del texto libre Docente.areaConocimiento).
     */
    private Docente obtenerDocenteElegible(Integer idDocente, Integer idCarrera, String rol) {
        Docente docente = docenteRepo.findById(idDocente)
                .orElseThrow(() -> new EntityNotFoundException(rol + " no encontrado: " + idDocente));

        if (docente.getTipoVinculacion() != TipoVinculacion.PLANTA) {
            throw new IllegalArgumentException(rol + " debe ser docente de planta UTEQ");
        }
        if (docente.getNivelPosgrado() == null || docente.getNivelPosgrado() == NivelPosgrado.NINGUNO) {
            throw new IllegalArgumentException(rol + " debe contar con título de posgrado");
        }
        boolean vinculadoACarrera = docenteCarreraRepo
                .existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(idDocente, idCarrera);
        if (!vinculadoACarrera) {
            throw new IllegalArgumentException(rol + " no está vinculado a la carrera de la propuesta (área afín)");
        }
        return docente;
    }

    private void alertarSiPlazoVencido(PropuestaTitulacion propuesta) {
        if (propuesta.getFechaRecepcionComision() == null) return;
        long dias = CalendarioAcademicoUtil.diasHabilesEntre(
                propuesta.getFechaRecepcionComision(), LocalDateTime.now());
        if (dias > DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO) {
            log.warn("Propuesta {}: el Consejo Directivo resolvió {} días hábiles después de la recepción de Comisión (límite reglamentario: {})",
                    propuesta.getIdPropuesta(), dias, DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO);
        }
    }

    private String nombreCompleto(Docente d) {
        if (d.getUsuario() == null) return "";
        return (d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()).trim();
    }
}
