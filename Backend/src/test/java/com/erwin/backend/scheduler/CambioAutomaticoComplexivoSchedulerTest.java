package com.erwin.backend.scheduler;

import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
import com.erwin.backend.repository.*;
import com.erwin.backend.service.EleccionService;
import com.erwin.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Art. 50 del reglamento: cambio automático a Examen Complexivo cuando un
 * estudiante en TIC no presenta su propuesta antes de la semana 04.
 */
@ExtendWith(MockitoExtension.class)
class CambioAutomaticoComplexivoSchedulerTest {

    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private MatriculaTitulacionRepository matriculaRepo;
    @Mock private ModalidadTitulacionRepository modalidadRepo;
    @Mock private PropuestaTitulacionRepository propuestaRepo;
    @Mock private EleccionTitulacionRepository eleccionRepo;
    @Mock private CoordinadorRepository coordinadorRepo;
    @Mock private EleccionService eleccionService;
    @Mock private EmailService emailService;

    private CambioAutomaticoComplexivoScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CambioAutomaticoComplexivoScheduler(
                periodoRepo, matriculaRepo, modalidadRepo, propuestaRepo, eleccionRepo,
                coordinadorRepo, eleccionService, emailService);
    }

    private PeriodoTitulacion periodo(LocalDate fechaInicio) {
        PeriodoTitulacion p = new PeriodoTitulacion();
        p.setIdPeriodo(1);
        p.setDescripcion("2026-1");
        p.setFechaInicio(fechaInicio);
        return p;
    }

    private MatriculaTitulacion matriculaTic(int idEstudiante) {
        Usuario u = new Usuario();
        u.setNombres("Ana");
        u.setApellidos("Perez");
        u.setCorreoInstitucional("ana@uteq.edu.ec");

        Estudiante e = new Estudiante();
        e.setIdEstudiante(idEstudiante);
        e.setUsuario(u);
        Carrera c = new Carrera();
        c.setIdCarrera(5);
        e.setCarrera(c);

        Modalidadtitulacion opcionTic = new Modalidadtitulacion();
        opcionTic.setIdModalidad(2);
        opcionTic.setNombre("Trabajo de Integración Curricular");

        MatriculaTitulacion m = new MatriculaTitulacion();
        m.setId(100L);
        m.setEstudiante(e);
        m.setNivel(NivelMatricula.TITULACION_I);
        m.setEstado(EstadoMatricula.ACTIVA);
        m.setPeriodoAcademico("2026-1");
        m.setOpcionTitulacion(opcionTic);
        return m;
    }

    @Test
    @DisplayName("No hace nada si todavía está dentro de la semana 04 del periodo")
    void noHaceNadaDentroDePlazo() {
        when(periodoRepo.findByActivoTrue()).thenReturn(Optional.of(periodo(LocalDate.now().minusDays(10))));

        scheduler.ejecutar();

        verifyNoInteractions(matriculaRepo, modalidadRepo, eleccionService, emailService);
    }

    @Test
    @DisplayName("Cambia a Examen Complexivo al estudiante que no presentó propuesta tras la semana 04")
    void cambiaAComplexivoSinPropuesta() {
        PeriodoTitulacion periodoActivo = periodo(LocalDate.now().minusDays(40));
        when(periodoRepo.findByActivoTrue()).thenReturn(Optional.of(periodoActivo));

        Modalidadtitulacion complexivo = new Modalidadtitulacion();
        complexivo.setIdModalidad(1);
        complexivo.setNombre("Examen Complexivo");
        when(modalidadRepo.findByNombreIgnoreCase("Examen Complexivo")).thenReturn(Optional.of(complexivo));

        MatriculaTitulacion matricula = matriculaTic(10);
        when(matriculaRepo.findByPeriodoAcademicoAndNivelAndEstado("2026-1", NivelMatricula.TITULACION_I, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matricula));

        EleccionTitulacion eleccion = new EleccionTitulacion();
        eleccion.setIdEleccion(500);
        when(eleccionRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(10, 1)).thenReturn(Optional.of(eleccion));
        when(propuestaRepo.findByEleccion_IdEleccion(500)).thenReturn(Optional.empty());
        when(coordinadorRepo.findFirstByCarrera_IdCarreraAndActivoTrue(5)).thenReturn(Optional.empty());
        when(matriculaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.ejecutar();

        verify(matriculaRepo).save(matricula);
        org.assertj.core.api.Assertions.assertThat(matricula.getOpcionTitulacion().getNombre()).isEqualTo("Examen Complexivo");
        org.assertj.core.api.Assertions.assertThat(matricula.getMotivoUltimoCambio()).contains("Art. 50");
        verify(eleccionService).elegir(eq(10), eq(1), anyString());
        verify(emailService, atLeastOnce()).notificarCambioOpcionTitulacion(eq("ana@uteq.edu.ec"), any(), any(), any());
    }

    @Test
    @DisplayName("No toca la matrícula si el estudiante ya presentó su propuesta")
    void noCambiaSiYaPresentoPropuesta() {
        PeriodoTitulacion periodoActivo = periodo(LocalDate.now().minusDays(40));
        when(periodoRepo.findByActivoTrue()).thenReturn(Optional.of(periodoActivo));

        Modalidadtitulacion complexivo = new Modalidadtitulacion();
        complexivo.setIdModalidad(1);
        complexivo.setNombre("Examen Complexivo");
        when(modalidadRepo.findByNombreIgnoreCase("Examen Complexivo")).thenReturn(Optional.of(complexivo));

        MatriculaTitulacion matricula = matriculaTic(20);
        when(matriculaRepo.findByPeriodoAcademicoAndNivelAndEstado("2026-1", NivelMatricula.TITULACION_I, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matricula));

        EleccionTitulacion eleccion = new EleccionTitulacion();
        eleccion.setIdEleccion(600);
        when(eleccionRepo.findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(20, 1)).thenReturn(Optional.of(eleccion));
        when(propuestaRepo.findByEleccion_IdEleccion(600)).thenReturn(Optional.of(new PropuestaTitulacion()));

        scheduler.ejecutar();

        verify(matriculaRepo, never()).save(any());
        verify(eleccionService, never()).elegir(anyInt(), anyInt(), any());
    }
}
