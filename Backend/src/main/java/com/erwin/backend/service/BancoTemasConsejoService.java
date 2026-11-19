package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.AprobarConsejoDirectivoRequest;
import com.erwin.backend.entities.BancoTemas;
import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoBancoTemas;
import com.erwin.backend.repository.BancoTemasRepository;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Última etapa del Módulo 5: el Consejo Directivo de Facultad aprueba un tema
 * que ya pasó por la Comisión de Investigación Formativa. Solo los temas en
 * APROBADO_CONSEJO_DIRECTIVO quedan disponibles para que un estudiante los
 * seleccione (ver ComisionTemasController.temasDisponiblesEstudiante).
 */
@Service
@Transactional
public class BancoTemasConsejoService {

    /**
     * Estados desde los que se puede resolver en Consejo Directivo. PROPUESTO
     * es, en la práctica actual del sistema, el estado en el que queda un tema
     * ya gestionado por un miembro de la Comisión (creado directamente por un
     * docente-comisión, o una sugerencia de estudiante ya aprobada por la
     * Comisión vía /sugerencias/{id}/aprobar) — no hay hoy un paso separado
     * que mueva PROPUESTO a APROBADO_COMISION, así que se acepta cualquiera
     * de los dos como punto de partida.
     */
    private static final Set<EstadoBancoTemas> ESTADOS_ELEGIBLES =
            EnumSet.of(EstadoBancoTemas.PROPUESTO, EstadoBancoTemas.APROBADO_COMISION);

    private final BancoTemasRepository bancoTemasRepo;
    private final UsuarioRepository usuarioRepo;
    private final CoordinadorRepository coordinadorRepo;

    public BancoTemasConsejoService(BancoTemasRepository bancoTemasRepo, UsuarioRepository usuarioRepo,
                                     CoordinadorRepository coordinadorRepo) {
        this.bancoTemasRepo = bancoTemasRepo;
        this.usuarioRepo = usuarioRepo;
        this.coordinadorRepo = coordinadorRepo;
    }

    @Auditable(entidad = "BancoTemas", accion = "APROBAR_CONSEJO_DIRECTIVO")
    public BancoTemas aprobarConsejoDirectivo(Integer idTema, AprobarConsejoDirectivoRequest req) {
        BancoTemas tema = bancoTemasRepo.findById(idTema)
                .orElseThrow(() -> new EntityNotFoundException("Tema no encontrado: " + idTema));

        if (!ESTADOS_ELEGIBLES.contains(tema.getEstado())) {
            throw new IllegalStateException(
                    "El tema debe estar aprobado por la Comisión antes de que el Consejo Directivo lo apruebe (estado actual: "
                            + tema.getEstado() + ")");
        }
        if (req == null || req.getIdUsuario() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario (Decano/Coordinador) que aprueba");
        }

        Usuario usuario = usuarioRepo.findById(req.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + req.getIdUsuario()));

        validarUsuarioPerteneceACarreraDelTema(usuario, tema);

        tema.setEstado(EstadoBancoTemas.APROBADO_CONSEJO_DIRECTIVO);
        tema.setAprobadoPorConsejo(usuario);
        tema.setFechaAprobacionConsejo(LocalDate.now());
        if (req.getObservaciones() != null && !req.getObservaciones().isBlank()) {
            tema.setObservaciones(req.getObservaciones().trim());
        }

        return bancoTemasRepo.save(tema);
    }

    /**
     * Igual que {@link com.erwin.backend.controller.ComisionTemasController#validarMiembroComision(Integer, Integer)}:
     * si el usuario que aprueba es Coordinador, su carrera debe coincidir con
     * la del tema (no puede aprobar temas de otra carrera). Un Decano/Admin no
     * tiene hoy una carrera/facultad asociada en el esquema, así que para
     * ellos no se restringe (misma limitación documentada para el resto del
     * sistema: no existe una relación Decano↔Facultad).
     */
    private void validarUsuarioPerteneceACarreraDelTema(Usuario usuario, BancoTemas tema) {
        coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(usuario.getIdUsuario()).ifPresent(coordinador -> {
            Integer idCarreraCoordinador = coordinadorCarreraId(coordinador);
            Integer idCarreraTema = tema.getCarrera() != null ? tema.getCarrera().getIdCarrera() : null;
            if (idCarreraCoordinador == null || !idCarreraCoordinador.equals(idCarreraTema)) {
                throw new AccessDeniedException(
                        "El usuario no pertenece a la carrera de este tema");
            }
        });
    }

    private Integer coordinadorCarreraId(Coordinador coordinador) {
        return coordinador.getCarrera() != null ? coordinador.getCarrera().getIdCarrera() : null;
    }
}
