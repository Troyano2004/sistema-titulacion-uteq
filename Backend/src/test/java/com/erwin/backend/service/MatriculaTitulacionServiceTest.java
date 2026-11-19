package com.erwin.backend.service;

import com.erwin.backend.dtos.CambiarOpcionMatriculaRequest;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.MatriculaTitulacion;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.MatriculaTitulacionRepository;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.PensumImportadoRepository;
import com.erwin.backend.repository.RequisitoImportadoRepository;
import com.erwin.backend.repository.TipoTrabajoTitulacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de cambio de opción de titulación en Titulación I
 * (MatriculaTitulacionService.cambiarOpcion): límite de 1 cambio, plazo de la
 * semana 4, y la excepción por caso médico fuera de plazo.
 */
@ExtendWith(MockitoExtension.class)
class MatriculaTitulacionServiceTest {

    @Mock private MatriculaTitulacionRepository matriculaRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private ModalidadTitulacionRepository modalidadRepo;
    @Mock private TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    @Mock private CoordinadorRepository coordinadorRepo;
    @Mock private EmailService emailService;
    @Mock private PensumImportadoRepository pensumImportadoRepo;
    @Mock private RequisitoImportadoRepository requisitoImportadoRepo;

    private MatriculaTitulacionService service;

    @BeforeEach
    void setUp() {
        service = new MatriculaTitulacionService(
                matriculaRepo, estudianteRepo, modalidadRepo, tipoTrabajoRepo, coordinadorRepo, emailService,
                pensumImportadoRepo, requisitoImportadoRepo);
    }

    private MatriculaTitulacion matriculaActivaTitulacionI(int numeroCambiosPrevios) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(1);
        estudiante.setCarrera(null); // sin carrera => la notificación al coordinador se omite

        MatriculaTitulacion m = new MatriculaTitulacion();
        m.setId(10L);
        m.setEstudiante(estudiante);
        m.setNivel(NivelMatricula.TITULACION_I);
        m.setEstado(EstadoMatricula.ACTIVA);
        m.setNumeroCambiosOpcion(numeroCambiosPrevios);
        return m;
    }

    private Modalidadtitulacion modalidadComplexivo() {
        Modalidadtitulacion m = new Modalidadtitulacion();
        m.setIdModalidad(2);
        m.setNombre("Examen Complexivo");
        return m;
    }

    @Test
    @DisplayName("Rechaza el cambio si ya se usó el único cambio permitido (numeroCambiosOpcion >= 1)")
    void rechazaSiYaAgotoElCambio() {
        MatriculaTitulacion matricula = matriculaActivaTitulacionI(1);
        when(matriculaRepo.findById(10L)).thenReturn(Optional.of(matricula));

        CambiarOpcionMatriculaRequest req = new CambiarOpcionMatriculaRequest();
        req.setIdOpcionTitulacion(2);
        req.setSemanaActual(2);

        assertThatThrownBy(() -> service.cambiarOpcion(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("máximo de 1 cambio");
    }

    @Test
    @DisplayName("Permite el cambio dentro de la semana 4 sin requerir motivo médico")
    void permiteCambioDentroDeSemana4() {
        MatriculaTitulacion matricula = matriculaActivaTitulacionI(0);
        when(matriculaRepo.findById(10L)).thenReturn(Optional.of(matricula));
        when(modalidadRepo.findById(2)).thenReturn(Optional.of(modalidadComplexivo()));
        when(matriculaRepo.save(any(MatriculaTitulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarOpcionMatriculaRequest req = new CambiarOpcionMatriculaRequest();
        req.setIdOpcionTitulacion(2);
        req.setSemanaActual(4);
        req.setMotivo("Cambio de preferencia");

        var response = service.cambiarOpcion(10L, req);

        assertThat(response.getNumeroCambiosOpcion()).isEqualTo(1);
        assertThat(response.getMotivoUltimoCambio()).isEqualTo("Cambio de preferencia");
        assertThat(matricula.getDocumentoAdjuntoIdCasoMedico()).isNull();
    }

    @Test
    @DisplayName("Rechaza el cambio fuera de semana 4 si no se acredita caso médico con documento")
    void rechazaFueraDePlazoSinCasoMedico() {
        MatriculaTitulacion matricula = matriculaActivaTitulacionI(0);
        when(matriculaRepo.findById(10L)).thenReturn(Optional.of(matricula));

        CambiarOpcionMatriculaRequest req = new CambiarOpcionMatriculaRequest();
        req.setIdOpcionTitulacion(2);
        req.setSemanaActual(5);

        assertThatThrownBy(() -> service.cambiarOpcion(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("semana 4");
    }

    @Test
    @DisplayName("Permite el cambio fuera de semana 4 cuando se acredita caso médico con documento")
    void permiteCambioFueraDePlazoConCasoMedico() {
        MatriculaTitulacion matricula = matriculaActivaTitulacionI(0);
        when(matriculaRepo.findById(10L)).thenReturn(Optional.of(matricula));
        when(modalidadRepo.findById(2)).thenReturn(Optional.of(modalidadComplexivo()));
        when(matriculaRepo.save(any(MatriculaTitulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarOpcionMatriculaRequest req = new CambiarOpcionMatriculaRequest();
        req.setIdOpcionTitulacion(2);
        req.setSemanaActual(6);
        req.setMotivoMedico("Reposo médico certificado");
        req.setDocumentoAdjuntoId(555L);

        var response = service.cambiarOpcion(10L, req);

        assertThat(response.getNumeroCambiosOpcion()).isEqualTo(1);
        assertThat(response.getMotivoUltimoCambio()).contains("CASO_MEDICO").contains("Reposo médico certificado");
        assertThat(response.getDocumentoAdjuntoIdCasoMedico()).isEqualTo(555L);
    }

    @Test
    @DisplayName("Rechaza el cambio de opción en matrículas de Titulación II")
    void rechazaCambioEnTitulacionII() {
        MatriculaTitulacion matricula = matriculaActivaTitulacionI(0);
        matricula.setNivel(NivelMatricula.TITULACION_II);
        when(matriculaRepo.findById(10L)).thenReturn(Optional.of(matricula));

        CambiarOpcionMatriculaRequest req = new CambiarOpcionMatriculaRequest();
        req.setIdOpcionTitulacion(2);
        req.setSemanaActual(2);

        assertThatThrownBy(() -> service.cambiarOpcion(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Titulación II");
    }
}
