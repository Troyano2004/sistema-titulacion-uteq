package com.erwin.backend.service;

import com.erwin.backend.dtos.JustificacionDtos.SolicitarJustificacionRequest;
import com.erwin.backend.dtos.JustificacionDtos.SolicitudJustificacionDto;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Módulo 14: plazo de 72h laborables (inasistencia al Complexivo), plazo de
 * 3 días hábiles (inasistencia a sustentación), y la regla de que solo se
 * permite una solicitud de caso médico grave por estudiante.
 */
@ExtendWith(MockitoExtension.class)
class JustificacionServiceTest {

    @Mock private SolicitudJustificacionRepository repo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private FeriadoAcademicoRepository feriadoRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private OportunidadService oportunidadService;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private JustificacionService service;

    @BeforeEach
    void setUp() {
        service = new JustificacionService(repo, estudianteRepo, usuarioRepo, feriadoRepo, periodoRepo, oportunidadService, authorizationService);
    }

    private Estudiante estudiante(int id) {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(id);
        Usuario u = new Usuario();
        u.setNombres("Carla");
        u.setApellidos("Ruiz");
        e.setUsuario(u);
        return e;
    }

    @Test
    @DisplayName("solicitar rechaza una segunda solicitud de caso médico grave para el mismo estudiante")
    void rechazaSegundaSolicitudDeCasoMedicoGrave() {
        when(estudianteRepo.findById(1)).thenReturn(Optional.of(estudiante(1)));
        when(repo.existsByEstudiante_IdEstudianteAndTipo(1, com.erwin.backend.enums.TipoJustificacion.CASO_MEDICO_GRAVE))
                .thenReturn(true);

        SolicitarJustificacionRequest req = new SolicitarJustificacionRequest(
                1, "CASO_MEDICO_GRAVE", "Hospitalización por apendicitis", 55L,
                "CERTIFICADO_IESS", LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.solicitar(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("solo se permite una");

        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("solicitar rechaza fuera del plazo de 72 horas laborables para inasistencia al Complexivo")
    void rechazaInasistenciaComplexivoFueraDePlazo() {
        when(estudianteRepo.findById(2)).thenReturn(Optional.of(estudiante(2)));
        when(feriadoRepo.fechasComoSet()).thenReturn(Set.of());

        // El examen fue hace 20 días -> 72 horas laborables (9 jornadas ~ 2 semanas) ya vencieron
        SolicitarJustificacionRequest req = new SolicitarJustificacionRequest(
                2, "INASISTENCIA_COMPLEXIVO", "No pude asistir por un accidente", null, null,
                LocalDateTime.now().minusDays(20));

        assertThatThrownBy(() -> service.solicitar(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fuera de plazo");
    }

    @Test
    @DisplayName("solicitar acepta una inasistencia a sustentación dentro del plazo de 3 días hábiles")
    void aceptaInasistenciaSustentacionDentroDePlazo() {
        when(estudianteRepo.findById(3)).thenReturn(Optional.of(estudiante(3)));
        when(feriadoRepo.fechasComoSet()).thenReturn(Set.of());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitarJustificacionRequest req = new SolicitarJustificacionRequest(
                3, "INASISTENCIA_SUSTENTACION", "Justificación avalada por Departamento Médico UTEQ",
                null, null, LocalDateTime.now());

        SolicitudJustificacionDto dto = service.solicitar(req);

        assertThat(dto.estado()).isEqualTo("PENDIENTE");
        assertThat(dto.fechaLimite()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("solicitar rechaza una inasistencia a sustentación fuera del plazo de 3 días hábiles")
    void rechazaInasistenciaSustentacionFueraDePlazo() {
        when(estudianteRepo.findById(4)).thenReturn(Optional.of(estudiante(4)));
        when(feriadoRepo.fechasComoSet()).thenReturn(Set.of());

        SolicitarJustificacionRequest req = new SolicitarJustificacionRequest(
                4, "INASISTENCIA_SUSTENTACION", "Justificación tardía", null, null,
                LocalDateTime.now().minusDays(10));

        assertThatThrownBy(() -> service.solicitar(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fuera de plazo");
    }
}
