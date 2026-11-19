package com.erwin.backend.service;

import com.erwin.backend.dtos.OportunidadDtos.ElegibilidadDto;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.entities.RegistroOportunidad;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoActualizacionConocimientos;
import com.erwin.backend.enums.ModalidadOportunidad;
import com.erwin.backend.enums.ResultadoOportunidad;
import com.erwin.backend.enums.TipoOportunidad;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Módulo 13: conteo de oportunidades por periodo (máximo 2) y control del
 * plazo de 5 años desde el último periodo de titulación.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadServiceTest {

    @Mock private RegistroOportunidadRepository registroRepo;
    @Mock private ActualizacionConocimientosRepository actualizacionRepo;
    @Mock private AutorizacionTerceraMatriculaRepository autorizacionRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private OportunidadService service;

    @BeforeEach
    void setUp() {
        service = new OportunidadService(registroRepo, actualizacionRepo, autorizacionRepo,
                estudianteRepo, periodoRepo, usuarioRepo, authorizationService);
    }

    private Estudiante estudiante(int id) {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(id);
        Usuario u = new Usuario();
        u.setNombres("Luis");
        u.setApellidos("Mora");
        e.setUsuario(u);
        return e;
    }

    private PeriodoTitulacion periodo(int id, LocalDate inicio, LocalDate fin) {
        PeriodoTitulacion p = new PeriodoTitulacion();
        p.setIdPeriodo(id);
        p.setDescripcion("P" + id);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setActivo(true);
        return p;
    }

    private RegistroOportunidad registro(Estudiante est, PeriodoTitulacion periodo,
                                          ModalidadOportunidad modalidad, TipoOportunidad tipo,
                                          ResultadoOportunidad resultado) {
        RegistroOportunidad r = new RegistroOportunidad();
        r.setEstudiante(est);
        r.setPeriodoTitulacion(periodo);
        r.setModalidad(modalidad);
        r.setTipoOportunidad(tipo);
        r.setResultado(resultado);
        r.setFecha(LocalDate.now());
        return r;
    }

    @Test
    @DisplayName("puedeRendir rechaza si ya hay 2 oportunidades registradas en el periodo actual")
    void rechazaSiYaHayDosOportunidadesEnElPeriodo() {
        Estudiante est = estudiante(1);
        PeriodoTitulacion periodoActivo = periodo(10, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(4));

        List<RegistroOportunidad> historial = List.of(
                registro(est, periodoActivo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.REPROBADO),
                registro(est, periodoActivo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.SUPLETORIO, ResultadoOportunidad.REPROBADO)
        );

        when(estudianteRepo.findById(1)).thenReturn(Optional.of(est));
        when(periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()).thenReturn(Optional.of(periodoActivo));
        when(registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(1)).thenReturn(historial);

        ElegibilidadDto resultado = service.puedeRendir(1, TipoOportunidad.FINAL);

        assertThat(resultado.puede()).isFalse();
        assertThat(resultado.motivo()).contains("2 oportunidades");
        assertThat(resultado.oportunidadesUsadasEnPeriodo()).isEqualTo(2);
    }

    @Test
    @DisplayName("puedeRendir permite un segundo intento (SUPLETORIO) cuando solo hay 1 oportunidad en el periodo")
    void permiteSegundoIntentoConUnaSolaOportunidadPrevia() {
        Estudiante est = estudiante(2);
        PeriodoTitulacion periodoActivo = periodo(11, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(4));

        List<RegistroOportunidad> historial = List.of(
                registro(est, periodoActivo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.REPROBADO)
        );

        when(estudianteRepo.findById(2)).thenReturn(Optional.of(est));
        when(periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()).thenReturn(Optional.of(periodoActivo));
        when(registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(2)).thenReturn(historial);

        ElegibilidadDto resultado = service.puedeRendir(2, TipoOportunidad.SUPLETORIO);

        assertThat(resultado.puede()).isTrue();
        assertThat(resultado.oportunidadesUsadasEnPeriodo()).isEqualTo(1);
    }

    @Test
    @DisplayName("control de 5 años: exige actualización de conocimientos si el último periodo terminó hace más de 5 años")
    void exigeActualizacionConocimientosSiPasaronMasDeCincoAnios() {
        Estudiante est = estudiante(3);
        PeriodoTitulacion periodoViejo = periodo(1,
                LocalDate.now().minusYears(7), LocalDate.now().minusYears(6));
        PeriodoTitulacion periodoActivo = periodo(20, LocalDate.now().minusDays(10), LocalDate.now().plusMonths(5));

        List<RegistroOportunidad> historial = List.of(
                registro(est, periodoViejo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.APROBADO)
        );

        when(estudianteRepo.findById(3)).thenReturn(Optional.of(est));
        when(periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()).thenReturn(Optional.of(periodoActivo));
        when(registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(3)).thenReturn(historial);
        when(actualizacionRepo.findFirstByEstudiante_IdEstudianteAndEstadoOrderByFechaSolicitudDesc(
                3, EstadoActualizacionConocimientos.APROBADA)).thenReturn(Optional.empty());

        ElegibilidadDto resultado = service.puedeRendir(3, TipoOportunidad.FINAL);

        assertThat(resultado.puede()).isFalse();
        assertThat(resultado.requiereActualizacionConocimientos()).isTrue();
        assertThat(resultado.motivo()).contains("5 años");
    }

    @Test
    @DisplayName("control de 5 años: permite el intento si ya existe una actualización de conocimientos APROBADA")
    void permiteElIntentoConActualizacionDeConocimientosAprobada() {
        Estudiante est = estudiante(4);
        PeriodoTitulacion periodoViejo = periodo(2,
                LocalDate.now().minusYears(7), LocalDate.now().minusYears(6));
        PeriodoTitulacion periodoActivo = periodo(21, LocalDate.now().minusDays(10), LocalDate.now().plusMonths(5));

        List<RegistroOportunidad> historial = List.of(
                registro(est, periodoViejo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.APROBADO)
        );

        when(estudianteRepo.findById(4)).thenReturn(Optional.of(est));
        when(periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()).thenReturn(Optional.of(periodoActivo));
        when(registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(4)).thenReturn(historial);
        when(actualizacionRepo.findFirstByEstudiante_IdEstudianteAndEstadoOrderByFechaSolicitudDesc(
                4, EstadoActualizacionConocimientos.APROBADA))
                .thenReturn(Optional.of(new com.erwin.backend.entities.ActualizacionConocimientos()));

        ElegibilidadDto resultado = service.puedeRendir(4, TipoOportunidad.FINAL);

        assertThat(resultado.puede()).isTrue();
    }

    @Test
    @DisplayName("el SUPLETORIO se rechaza si el estudiante ya usó un periodo de titulación anterior")
    void rechazaSupletorioFueraDeLaPrimeraOcasion() {
        Estudiante est = estudiante(5);
        PeriodoTitulacion periodoViejo = periodo(3, LocalDate.now().minusYears(1), LocalDate.now().minusMonths(7));
        PeriodoTitulacion periodoActivo = periodo(22, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(4));

        List<RegistroOportunidad> historial = List.of(
                registro(est, periodoViejo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.REPROBADO),
                registro(est, periodoActivo, ModalidadOportunidad.EXAMEN_COMPLEXIVO, TipoOportunidad.FINAL, ResultadoOportunidad.REPROBADO)
        );

        when(estudianteRepo.findById(5)).thenReturn(Optional.of(est));
        when(periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()).thenReturn(Optional.of(periodoActivo));
        when(registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(5)).thenReturn(historial);

        ElegibilidadDto resultado = service.puedeRendir(5, TipoOportunidad.SUPLETORIO);

        assertThat(resultado.puede()).isFalse();
        assertThat(resultado.motivo()).contains("primera ocasión");
    }
}
