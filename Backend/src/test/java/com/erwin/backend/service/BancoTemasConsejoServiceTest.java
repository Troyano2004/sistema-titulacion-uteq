package com.erwin.backend.service;

import com.erwin.backend.dtos.AprobarConsejoDirectivoRequest;
import com.erwin.backend.entities.BancoTemas;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoBancoTemas;
import com.erwin.backend.repository.BancoTemasRepository;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * FIX 3 de la verificación funcional: aprobarConsejoDirectivo() debe validar
 * que el usuario que aprueba pertenezca a la carrera del tema (mismo patrón
 * que ComisionTemasController.validarMiembroComision).
 */
@ExtendWith(MockitoExtension.class)
class BancoTemasConsejoServiceTest {

    @Mock private BancoTemasRepository bancoTemasRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private CoordinadorRepository coordinadorRepo;

    private BancoTemasConsejoService service;

    @BeforeEach
    void setUp() {
        service = new BancoTemasConsejoService(bancoTemasRepo, usuarioRepo, coordinadorRepo);
    }

    private BancoTemas temaDeCarrera(Integer idCarrera, EstadoBancoTemas estado) {
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(idCarrera);
        BancoTemas tema = new BancoTemas();
        ReflectionTestUtils.setField(tema, "idTema", 1);
        tema.setCarrera(carrera);
        tema.setEstado(estado);
        return tema;
    }

    private Usuario usuario(Integer id) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "idUsuario", id);
        return u;
    }

    private Coordinador coordinadorDeCarrera(Integer idCarrera) {
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(idCarrera);
        Coordinador coordinador = new Coordinador();
        coordinador.setCarrera(carrera);
        return coordinador;
    }

    @Test
    @DisplayName("Coordinador de la misma carrera del tema puede aprobar en Consejo Directivo")
    void coordinadorDeLaMismaCarreraPuedeAprobar() {
        BancoTemas tema = temaDeCarrera(2, EstadoBancoTemas.PROPUESTO);
        when(bancoTemasRepo.findById(1)).thenReturn(Optional.of(tema));
        when(usuarioRepo.findById(108)).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDeCarrera(2)));
        when(bancoTemasRepo.save(tema)).thenReturn(tema);

        AprobarConsejoDirectivoRequest req = new AprobarConsejoDirectivoRequest();
        req.setIdUsuario(108);

        BancoTemas resultado = service.aprobarConsejoDirectivo(1, req);

        assertThat(resultado.getEstado()).isEqualTo(EstadoBancoTemas.APROBADO_CONSEJO_DIRECTIVO);
    }

    @Test
    @DisplayName("Coordinador de OTRA carrera no puede aprobar el tema — 403")
    void coordinadorDeOtraCarreraNoPuedeAprobar() {
        BancoTemas tema = temaDeCarrera(2, EstadoBancoTemas.PROPUESTO);
        when(bancoTemasRepo.findById(1)).thenReturn(Optional.of(tema));
        when(usuarioRepo.findById(108)).thenReturn(Optional.of(usuario(108)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(108))
                .thenReturn(Optional.of(coordinadorDeCarrera(3)));

        AprobarConsejoDirectivoRequest req = new AprobarConsejoDirectivoRequest();
        req.setIdUsuario(108);

        assertThatThrownBy(() -> service.aprobarConsejoDirectivo(1, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Un Decano/Admin (sin fila Coordinador activa) no está restringido por carrera")
    void usuarioSinFilaCoordinadorNoEstaRestringidoPorCarrera() {
        BancoTemas tema = temaDeCarrera(2, EstadoBancoTemas.APROBADO_COMISION);
        when(bancoTemasRepo.findById(1)).thenReturn(Optional.of(tema));
        when(usuarioRepo.findById(47)).thenReturn(Optional.of(usuario(47)));
        when(coordinadorRepo.findByUsuario_IdUsuarioAndActivoTrue(47)).thenReturn(Optional.empty());
        when(bancoTemasRepo.save(tema)).thenReturn(tema);

        AprobarConsejoDirectivoRequest req = new AprobarConsejoDirectivoRequest();
        req.setIdUsuario(47);

        assertThatCode(() -> service.aprobarConsejoDirectivo(1, req)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un tema en estado RECHAZADO no puede aprobarse en Consejo Directivo")
    void temaEnEstadoNoElegibleLanzaExcepcion() {
        BancoTemas tema = temaDeCarrera(2, EstadoBancoTemas.RECHAZADO);
        when(bancoTemasRepo.findById(1)).thenReturn(Optional.of(tema));

        AprobarConsejoDirectivoRequest req = new AprobarConsejoDirectivoRequest();
        req.setIdUsuario(108);

        assertThatThrownBy(() -> service.aprobarConsejoDirectivo(1, req))
                .isInstanceOf(IllegalStateException.class);
    }
}
