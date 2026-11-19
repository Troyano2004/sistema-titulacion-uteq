package com.erwin.backend.service;

import com.erwin.backend.dtos.AntiplagioResultadoResponse;
import com.erwin.backend.dtos.RegistrarResultadoAntiplagioRequest;
import com.erwin.backend.entities.AntiplacioIntento;
import com.erwin.backend.entities.Docente;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.repository.AntiplacioIntentoRepository;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Módulo 8: bloqueo estricto por COMPILATIO >= 10% de coincidencia — la
 * emisión del certificado antiplagio debe rechazarse aunque el registro del
 * resultado en sí nunca lance excepción (se necesita poder dejar constancia
 * de un intento desfavorable para que el estudiante corrija el documento).
 */
@ExtendWith(MockitoExtension.class)
class AntiplagioCompilatioServiceTest {

    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private DocumentoTitulacionRepository documentoRepo;
    @Mock private AntiplacioIntentoRepository antiplagioRepo;

    private AntiplagioCompilatioService service;

    @BeforeEach
    void setUp() {
        service = new AntiplagioCompilatioService(proyectoRepo, documentoRepo, antiplagioRepo);
    }

    private ProyectoTitulacion proyectoConDirector(int idProyecto) {
        Docente director = new Docente();
        director.setIdDocente(5);

        ProyectoTitulacion proyecto = new ProyectoTitulacion();
        proyecto.setIdProyecto(idProyecto);
        proyecto.setDirector(director);
        return proyecto;
    }

    @Test
    @DisplayName("registrarResultado con porcentaje >= 10% no avanza a predefensa, pero sí registra el intento")
    void registrarResultadoConPorcentajeAltoNoAvanzaAPredefensa() {
        ProyectoTitulacion proyecto = proyectoConDirector(1);
        when(proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(30))
                .thenReturn(Optional.of(proyecto));
        when(antiplagioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarResultadoAntiplagioRequest req = new RegistrarResultadoAntiplagioRequest();
        req.setEstudianteId(30);
        req.setPorcentaje(new BigDecimal("15.00"));
        req.setObservaciones("Revisar citas");
        req.setArchivoReporteId(999L);

        AntiplagioResultadoResponse resp = service.registrarResultado(req);

        assertThat(resp.favorable()).isFalse();
        assertThat(resp.mensaje()).contains("supera el máximo permitido (10%)");
        verify(documentoRepo, never()).findByProyecto_IdProyecto(any());
    }

    @Test
    @DisplayName("emitirCertificado rechaza si el último porcentaje registrado es >= 10%")
    void emitirCertificadoRechazaSiPorcentajeAlto() {
        ProyectoTitulacion proyecto = proyectoConDirector(2);
        when(proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(31))
                .thenReturn(Optional.of(proyecto));

        AntiplacioIntento ultimo = new AntiplacioIntento();
        ultimo.setPorcentajeCoincidencia(new BigDecimal("12.00"));
        when(antiplagioRepo.findByProyecto_IdProyectoOrderByFechaIntentoDesc(2)).thenReturn(List.of(ultimo));

        assertThatThrownBy(() -> service.emitirCertificado(31))
                .hasMessageContaining("supera el máximo permitido (10%)");

        verify(antiplagioRepo, never()).save(any());
    }

    @Test
    @DisplayName("emitirCertificado emite el certificado si el último porcentaje es < 10%")
    void emitirCertificadoEmiteSiPorcentajeFavorable() {
        ProyectoTitulacion proyecto = proyectoConDirector(3);
        when(proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(32))
                .thenReturn(Optional.of(proyecto));

        AntiplacioIntento ultimo = new AntiplacioIntento();
        ultimo.setPorcentajeCoincidencia(new BigDecimal("5.00"));
        when(antiplagioRepo.findByProyecto_IdProyectoOrderByFechaIntentoDesc(3)).thenReturn(List.of(ultimo));
        when(antiplagioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.emitirCertificado(32);

        assertThat(resp.certificadoEmitido()).isTrue();
        assertThat(ultimo.getCertificadoEmitido()).isTrue();
    }
}
