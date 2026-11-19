package com.erwin.backend.service;

import com.erwin.backend.dtos.reporte.DashboardReportesDto;
import com.erwin.backend.dtos.reporte.ResultadosFacultadDto;
import com.erwin.backend.entities.Facultad;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * TAREA 2 — reportes institucionales: generación de resultados por facultad
 * y cálculo de tasa de aprobación (facultad y dashboard general). Las
 * agregaciones reales viven en consultas JPQL de los repositorios, así que
 * aquí se mockean los repositorios devolviendo filas ya agregadas (Object[])
 * y se verifica que el servicio las combine y calcule los porcentajes bien.
 */
@ExtendWith(MockitoExtension.class)
class ReportesInstitucionalesServiceTest {

    @Mock private FacultadRepository facultadRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private ExamenComplexivoAplicacionRepository examenRepo;
    @Mock private MatriculaTitulacionRepository matriculaRepo;
    @Mock private ActaGradoRepository actaGradoRepo;
    @Mock private ExpedienteEstudianteRepository expedienteRepo;
    @Mock private DocumentoExpedienteRepository documentoExpedienteRepo;
    @Mock private EleccionTitulacionRepository eleccionRepo;
    @Mock private PropuestaTitulacionRepository propuestaRepo;
    @Mock private AnteproyectoTitulacionRepository anteproyectoRepo;
    @Mock private TutoriaAnteproyectoRepository tutoriaRepo;
    @Mock private BitacoraAsignacionRepository bitacoraRepo;
    @Mock private AsesoriaDirectorRepository asesoriaRepo;
    @Mock private SustentacionRepository sustentacionRepo;
    @Mock private TituloProfesionalRepository tituloRepo;
    @Mock private CarreraRepository carreraRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private ReportesInstitucionalesService service;

    @BeforeEach
    void setUp() {
        service = new ReportesInstitucionalesService(facultadRepo, periodoRepo, estudianteRepo, proyectoRepo,
            examenRepo, matriculaRepo, actaGradoRepo, expedienteRepo, documentoExpedienteRepo, eleccionRepo,
            propuestaRepo, anteproyectoRepo, tutoriaRepo, bitacoraRepo, asesoriaRepo, sustentacionRepo,
            tituloRepo, carreraRepo, authorizationService);
    }

    @Test
    @DisplayName("getResultadosFacultad agrega TIC y Complexivo por carrera y calcula la tasa de aprobación")
    void generaReporteDeFacultadConTasaDeAprobacion() {
        Facultad facultad = new Facultad();
        facultad.setIdFacultad(1);
        facultad.setNombre("Facultad de Ciencias de la Ingeniería");
        when(facultadRepo.findById(1)).thenReturn(Optional.of(facultad));

        PeriodoTitulacion periodo = new PeriodoTitulacion();
        periodo.setIdPeriodo(5);
        periodo.setDescripcion("2025-2026 CI");
        when(periodoRepo.findByDescripcion("2025-2026 CI")).thenReturn(Optional.of(periodo));

        // Carrera 10: TIC -> 8 total, 6 aprobados, 2 reprobados
        List<Object[]> filasTic = Collections.singletonList(new Object[]{
            10, "Ingeniería en Sistemas", "TRABAJO_INTEGRACION_CURRICULAR", 8L, 6L, 2L, new BigDecimal("8.25")
        });
        when(proyectoRepo.resultadosPorCarreraYModalidad(1, "2025-2026 CI")).thenReturn(filasTic);

        // Misma carrera 10: Complexivo -> 2 total, 2 aprobados, 0 reprobados
        List<Object[]> filasComplexivo = Collections.singletonList(new Object[]{
            10, "Ingeniería en Sistemas", 2L, 2L, 0L, new BigDecimal("9.00")
        });
        when(examenRepo.resultadosPorCarrera(eq(1), eq("2025-2026 CI"), any(BigDecimal.class))).thenReturn(filasComplexivo);
        when(examenRepo.ultimaFechaExamenFinalizado(1, "2025-2026 CI")).thenReturn(null);

        ResultadosFacultadDto dto = service.getResultadosFacultad(1, "2025-2026 CI");

        assertThat(dto.getFacultad()).isEqualTo("Facultad de Ciencias de la Ingeniería");
        assertThat(dto.getTotalEstudiantes()).isEqualTo(10);
        assertThat(dto.getTotalAprobados()).isEqualTo(8);
        assertThat(dto.getTotalReprobados()).isEqualTo(2);
        // 8 aprobados de 10 -> 80% de tasa de aprobación general
        assertThat(dto.getTasaAprobacionGeneral()).isCloseTo(80.0, within(0.001));
        assertThat(dto.getDentroDePlazo()).isTrue();

        assertThat(dto.getCarreras()).hasSize(1);
        ResultadosFacultadDto.CarreraResultadoDto carrera = dto.getCarreras().get(0);
        assertThat(carrera.getCarrera()).isEqualTo("Ingeniería en Sistemas");
        assertThat(carrera.getTotalEstudiantes()).isEqualTo(10);
        assertThat(carrera.getTasaAprobacion()).isCloseTo(80.0, within(0.001));
        assertThat(carrera.getPorModalidad()).hasSize(2);
    }

    @Test
    @DisplayName("getDashboard calcula la tasa de aprobación institucional combinando TIC y Complexivo")
    void calculaTasaDeAprobacionEnDashboard() {
        when(matriculaRepo.conteoPorNivel("2025-2026 CI")).thenReturn(Collections.emptyList());

        // TIC: 10 total, 7 aprobados, 1 reprobado, 2 en proceso
        List<Object[]> ticFilas = Collections.singletonList(new Object[]{
            "TRABAJO_INTEGRACION_CURRICULAR", 10L, 7L, 1L, 2L
        });
        when(proyectoRepo.resumenPorModalidad("2025-2026 CI")).thenReturn(ticFilas);

        // Complexivo: 3 total, 2 aprobados, 1 reprobado
        List<Object[]> complexivoFila = Collections.singletonList(new Object[]{3L, 2L, 1L});
        when(examenRepo.resumenGlobal(eq("2025-2026 CI"), any(BigDecimal.class))).thenReturn(complexivoFila);

        when(actaGradoRepo.promedioNotaFinalGradoGlobal()).thenReturn(new BigDecimal("8.40"));
        when(matriculaRepo.fechasMatriculaYGradoParaTiempoPromedio()).thenReturn(Collections.emptyList());

        DashboardReportesDto dto = service.getDashboard("2025-2026 CI");

        // aprobados = 7 (TIC) + 2 (Complexivo) = 9; reprobados = 1 + 1 = 2; total evaluados = 11
        assertThat(dto.getAprobados()).isEqualTo(9);
        assertThat(dto.getReprobados()).isEqualTo(2);
        assertThat(dto.getTasaAprobacion()).isCloseTo(9 * 100.0 / 11, within(0.001));
        assertThat(dto.getPorModalidad()).hasSize(2);
    }
}
