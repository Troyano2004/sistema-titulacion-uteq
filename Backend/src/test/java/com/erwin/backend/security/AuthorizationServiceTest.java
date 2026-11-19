package com.erwin.backend.security;

import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.Facultad;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Cubre los chequeos de aislamiento introducidos para corregir las 4 fallas
 * de seguridad detectadas en la verificación E2E: un ESTUDIANTE no puede
 * acceder a datos de otro estudiante, y un COORDINADOR no puede acceder a
 * datos de otra carrera/facultad.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private UsuarioRepository usuarioRepo;
    @Mock private CoordinadorRepository coordinadorRepo;

    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(usuarioRepo, coordinadorRepo);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(username, null, authorities));
    }

    private Usuario usuario(Integer id) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "idUsuario", id);
        return u;
    }

    // ── exigirPropioEstudianteSiAplica ──────────────────────────────────────

    @Test
    void estudianteAccediendoASuPropioIdNoLanzaExcepcion() {
        autenticarComo("e1bueno", "ROLE_ESTUDIANTE");
        when(usuarioRepo.findByUsername("e1bueno")).thenReturn(Optional.of(usuario(104)));

        assertThatCode(() -> service.exigirPropioEstudianteSiAplica(104)).doesNotThrowAnyException();
    }

    @Test
    void estudianteAccediendoAOtroIdLanza403() {
        autenticarComo("e1bueno", "ROLE_ESTUDIANTE");
        when(usuarioRepo.findByUsername("e1bueno")).thenReturn(Optional.of(usuario(104)));

        assertThatThrownBy(() -> service.exigirPropioEstudianteSiAplica(105))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void estudianteConIdEstudianteNuloEnElRecursoLanza403() {
        // Caso BUG-9: el path apuntaba a un id que no coincide con el propio.
        autenticarComo("e1bueno", "ROLE_ESTUDIANTE");
        when(usuarioRepo.findByUsername("e1bueno")).thenReturn(Optional.of(usuario(104)));

        assertThatThrownBy(() -> service.exigirPropioEstudianteSiAplica(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void personalAdministrativoNoEstaRestringidoPorEstudiante() {
        autenticarComo("coord1", "ROLE_COORDINADOR");

        assertThatCode(() -> service.exigirPropioEstudianteSiAplica(999)).doesNotThrowAnyException();
    }

    @Test
    void sinAutenticacionNoLanzaExcepcion() {
        // Sin Authentication en el contexto (p.ej. tarea del scheduler): no hay
        // rol ESTUDIANTE que restringir.
        assertThatCode(() -> service.exigirPropioEstudianteSiAplica(1)).doesNotThrowAnyException();
    }

    // ── exigirPropiaCarreraSiCoordinador / exigirPropiaFacultadSiCoordinador ─

    private Coordinador coordinadorDe(Integer idCarrera, Integer idFacultad) {
        Facultad facultad = new Facultad();
        facultad.setIdFacultad(idFacultad);
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(idCarrera);
        carrera.setFacultad(facultad);
        Coordinador coordinador = new Coordinador();
        coordinador.setCarrera(carrera);
        return coordinador;
    }

    @Test
    void coordinadorAccediendoASuPropiaCarreraNoLanzaExcepcion() {
        autenticarComo("coord1", "ROLE_COORDINADOR");
        when(usuarioRepo.findByUsername("coord1")).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDe(2, 1)));

        assertThatCode(() -> service.exigirPropiaCarreraSiCoordinador(2)).doesNotThrowAnyException();
    }

    @Test
    void coordinadorAccediendoAOtraCarreraLanza403() {
        autenticarComo("coord1", "ROLE_COORDINADOR");
        when(usuarioRepo.findByUsername("coord1")).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDe(2, 1)));

        assertThatThrownBy(() -> service.exigirPropiaCarreraSiCoordinador(3))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void coordinadorAccediendoASuPropiaFacultadNoLanzaExcepcion() {
        autenticarComo("coord1", "ROLE_COORDINADOR");
        when(usuarioRepo.findByUsername("coord1")).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDe(2, 1)));

        assertThatCode(() -> service.exigirPropiaFacultadSiCoordinador(1)).doesNotThrowAnyException();
    }

    @Test
    void coordinadorAccediendoAOtraFacultadLanza403() {
        autenticarComo("coord1", "ROLE_COORDINADOR");
        when(usuarioRepo.findByUsername("coord1")).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDe(2, 1)));

        assertThatThrownBy(() -> service.exigirPropiaFacultadSiCoordinador(999))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void noCoordinadorNoEstaRestringidoPorCarreraNiFacultad() {
        autenticarComo("decano1", "ROLE_DECANO");

        assertThatCode(() -> service.exigirPropiaCarreraSiCoordinador(999)).doesNotThrowAnyException();
        assertThatCode(() -> service.exigirPropiaFacultadSiCoordinador(999)).doesNotThrowAnyException();
    }

    @Test
    void coordinadorSinFilaActivaEnBdLanza403() {
        // Rol COORDINADOR en el JWT pero sin registro Coordinador activo (dato inconsistente):
        // debe denegar, no asumir acceso total.
        autenticarComo("coordFantasma", "ROLE_COORDINADOR");
        when(usuarioRepo.findByUsername("coordFantasma")).thenReturn(Optional.of(usuario(200)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(200)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exigirPropiaCarreraSiCoordinador(2))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void esRolEstudianteYEsRolCoordinadorReflejanLasAuthorities() {
        autenticarComo("e1bueno", "ROLE_ESTUDIANTE");
        assertThat(service.esRolEstudiante()).isTrue();
        assertThat(service.esRolCoordinador()).isFalse();

        SecurityContextHolder.clearContext();
        autenticarComo("coord1", "ROLE_COORDINADOR");
        assertThat(service.esRolEstudiante()).isFalse();
        assertThat(service.esRolCoordinador()).isTrue();
    }
}
