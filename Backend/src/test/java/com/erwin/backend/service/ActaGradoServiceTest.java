package com.erwin.backend.service;

import com.erwin.backend.dtos.IncorporacionDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoActaGrado;
import com.erwin.backend.enums.EstadoDeudaImportada;
import com.erwin.backend.enums.EstadoExpediente;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** TAREA 1: cálculo de nota de grado 80/20 y generación del acta de incorporación. */
@ExtendWith(MockitoExtension.class)
class ActaGradoServiceTest {

    @Mock private ActaGradoRepository actaGradoRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private ExpedienteEstudianteRepository expedienteRepo;
    @Mock private DeudaImportadaRepository deudaRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private ActaGradoService service;
    private static final Integer ID_ESTUDIANTE = 10;

    @BeforeEach
    void setUp() {
        service = new ActaGradoService(actaGradoRepo, estudianteRepo, expedienteRepo, deudaRepo, authorizationService);
    }

    private Estudiante estudiante() {
        Usuario u = new Usuario();
        u.setNombres("Ana");
        u.setApellidos("Pérez");
        Estudiante e = new Estudiante();
        e.setIdEstudiante(ID_ESTUDIANTE);
        e.setUsuario(u);
        return e;
    }

    private ExpedienteEstudiante expediente(EstadoExpediente estado) {
        ExpedienteEstudiante ex = new ExpedienteEstudiante();
        ex.setId(1L);
        ex.setEstado(estado);
        return ex;
    }

    private ActaGrado actaConNotas(BigDecimal record, BigDecimal titulacion) {
        ActaGrado a = new ActaGrado();
        a.setIdActa(100);
        a.setEstudiante(estudiante());
        a.setTipoTitulacion("TRABAJO_INTEGRACION_CURRICULAR");
        a.setNotaRecord(record);
        a.setNotaTitulacion(titulacion);
        return a;
    }

    @Test
    @DisplayName("generar() calcula la nota final de grado como 80% récord + 20% titulación")
    void calculaNotaFinalDeGradoConFormula80_20() {
        when(estudianteRepo.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante()));
        when(expedienteRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(expediente(EstadoExpediente.COMPLETO)));
        when(deudaRepo.existsByEstudiante_IdEstudianteAndEstado(ID_ESTUDIANTE, EstadoDeudaImportada.PENDIENTE))
                .thenReturn(false);
        when(actaGradoRepo.findByEstudiante_IdEstudiante(ID_ESTUDIANTE))
                .thenReturn(Optional.of(actaConNotas(new BigDecimal("9.00"), new BigDecimal("8.00"))));
        when(actaGradoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActaGradoIncorporacionResponse resp = service.generar(ID_ESTUDIANTE);

        // 9.00*0.80 + 8.00*0.20 = 7.20 + 1.60 = 8.80
        assertThat(resp.notaFinalGrado()).isEqualByComparingTo("8.80");
        assertThat(resp.modalidad()).isEqualTo("TIC");
        assertThat(resp.estado()).isEqualTo("GENERADA");
    }

    @Test
    @DisplayName("generar() rechaza si el expediente no está COMPLETO")
    void rechazaSiElExpedienteNoEstaCompleto() {
        when(estudianteRepo.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante()));
        when(expedienteRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(expediente(EstadoExpediente.INCOMPLETO)));

        assertThatThrownBy(() -> service.generar(ID_ESTUDIANTE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("COMPLETO");
    }

    @Test
    @DisplayName("generar() rechaza si hay deudas administrativas pendientes importadas")
    void rechazaSiHayDeudasPendientes() {
        when(estudianteRepo.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante()));
        when(expedienteRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(expediente(EstadoExpediente.COMPLETO)));
        when(deudaRepo.existsByEstudiante_IdEstudianteAndEstado(ID_ESTUDIANTE, EstadoDeudaImportada.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.generar(ID_ESTUDIANTE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deudas");
    }

    @Test
    @DisplayName("generar() rechaza si el estudiante todavía no tiene nota de grado calculada")
    void rechazaSiNoHayActaPrevia() {
        when(estudianteRepo.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante()));
        when(expedienteRepo.findFirstByEstudiante_IdEstudianteOrderByIdDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(expediente(EstadoExpediente.COMPLETO)));
        when(deudaRepo.existsByEstudiante_IdEstudianteAndEstado(ID_ESTUDIANTE, EstadoDeudaImportada.PENDIENTE))
                .thenReturn(false);
        when(actaGradoRepo.findByEstudiante_IdEstudiante(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(ID_ESTUDIANTE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nota de grado calculada");
    }

    @Test
    @DisplayName("firmar() marca el acta como FIRMADA solo cuando las 3 firmas están registradas")
    void firmarMarcaFirmadaSoloConLasTresFirmas() {
        ActaGrado acta = actaConNotas(new BigDecimal("9.00"), new BigDecimal("8.00"));
        acta.setEstado(EstadoActaGrado.GENERADA);
        when(actaGradoRepo.findById(100)).thenReturn(Optional.of(acta));
        when(actaGradoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.firmar(100, new FirmarActaRequest("RECTOR"));
        assertThat(acta.getEstado()).isEqualTo(EstadoActaGrado.GENERADA);

        service.firmar(100, new FirmarActaRequest("DECANO"));
        assertThat(acta.getEstado()).isEqualTo(EstadoActaGrado.GENERADA);

        ActaGradoIncorporacionResponse resp = service.firmar(100, new FirmarActaRequest("SECRETARIO_GENERAL"));
        assertThat(resp.estado()).isEqualTo("FIRMADA");
    }
}
