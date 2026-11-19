package com.erwin.backend.security;

import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Chequeos de aislamiento de acceso:
 *  - Por ESTUDIANTE: evita que un usuario con ese rol acceda a datos de OTRO
 *    estudiante cambiando el id en la URL.
 *  - Por CARRERA/FACULTAD: evita que un COORDINADOR acceda a datos de una
 *    carrera o facultad que no es la suya.
 * El resto del personal (SECRETARIO_ABOGADO, DECANO, DOCENTE, ADMIN,
 * DIRECCION_ACADEMICA, etc.) no está sujeto a estas restricciones aquí — su
 * alcance se controla con @PreAuthorize por rol. Nota sobre DECANO: el
 * esquema actual no tiene una tabla que vincule un usuario DECANO con una
 * facultad concreta (no existe una entidad "Decano" — a diferencia de
 * Coordinador↔Carrera), así que no es posible acotarlo por facultad sin
 * ampliar el modelo de datos; queda pendiente como limitación conocida.
 */
@Service
public class AuthorizationService {

    private static final String ROLE_ESTUDIANTE = "ROLE_ESTUDIANTE";
    private static final String ROLE_COORDINADOR = "ROLE_COORDINADOR";

    private final UsuarioRepository usuarioRepo;
    private final CoordinadorRepository coordinadorRepo;

    public AuthorizationService(UsuarioRepository usuarioRepo, CoordinadorRepository coordinadorRepo) {
        this.usuarioRepo = usuarioRepo;
        this.coordinadorRepo = coordinadorRepo;
    }

    /** True si el usuario autenticado actual tiene el rol ESTUDIANTE. */
    public boolean esRolEstudiante() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_ESTUDIANTE.equals(a.getAuthority()));
    }

    /** id_usuario (== id_estudiante) del usuario autenticado, o null si no se puede resolver. */
    public Integer idUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return null;
        return usuarioRepo.findByUsername(auth.getName()).map(Usuario::getIdUsuario).orElse(null);
    }

    /**
     * Si el usuario autenticado tiene rol ESTUDIANTE, exige que idEstudiante
     * coincida con su propio id (id_estudiante == id_usuario). Los demás
     * roles pasan sin restricción aquí. Lanza AccessDeniedException (403)
     * si un estudiante intenta acceder al recurso de otro.
     */
    public void exigirPropioEstudianteSiAplica(Integer idEstudiante) {
        if (!esRolEstudiante()) return;
        Integer idAutenticado = idUsuarioAutenticado();
        if (idAutenticado == null || idEstudiante == null || !idAutenticado.equals(idEstudiante)) {
            throw new AccessDeniedException("No tiene permisos para acceder a información de otro estudiante");
        }
    }

    /** True si el usuario autenticado actual tiene el rol COORDINADOR. */
    public boolean esRolCoordinador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_COORDINADOR.equals(a.getAuthority()));
    }

    /** Coordinador activo asociado al usuario autenticado, o null si no aplica/no se encuentra. */
    private Coordinador coordinadorAutenticado() {
        Integer idUsuario = idUsuarioAutenticado();
        if (idUsuario == null) return null;
        return coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(idUsuario).orElse(null);
    }

    /**
     * Si el usuario autenticado tiene rol COORDINADOR, exige que idCarrera
     * corresponda a la carrera que coordina. Los demás roles (DECANO, ADMIN,
     * DIRECCION_ACADEMICA, etc.) pasan sin restricción aquí.
     */
    public void exigirPropiaCarreraSiCoordinador(Integer idCarrera) {
        if (!esRolCoordinador()) return;
        Coordinador coordinador = coordinadorAutenticado();
        if (coordinador == null || coordinador.getCarrera() == null
                || idCarrera == null || !coordinador.getCarrera().getIdCarrera().equals(idCarrera)) {
            throw new AccessDeniedException("No tiene permisos para acceder a información de otra carrera");
        }
    }

    /**
     * Si el usuario autenticado tiene rol COORDINADOR, exige que idFacultad
     * corresponda a la facultad de la carrera que coordina. Los demás roles
     * pasan sin restricción aquí (ver nota de clase sobre DECANO).
     */
    public void exigirPropiaFacultadSiCoordinador(Integer idFacultad) {
        if (!esRolCoordinador()) return;
        Coordinador coordinador = coordinadorAutenticado();
        if (coordinador == null || coordinador.getCarrera() == null
                || coordinador.getCarrera().getFacultad() == null || idFacultad == null
                || !coordinador.getCarrera().getFacultad().getIdFacultad().equals(idFacultad)) {
            throw new AccessDeniedException("No tiene permisos para acceder a información de otra facultad");
        }
    }
}
