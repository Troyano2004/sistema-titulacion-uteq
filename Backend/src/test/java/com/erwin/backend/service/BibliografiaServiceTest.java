package com.erwin.backend.service;

import com.erwin.backend.dtos.DocumentoTicDtos.BibliografiaFormateadaResponse;
import com.erwin.backend.dtos.DocumentoTicDtos.ReferenciaBibliograficaRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstandarBibliografico;
import com.erwin.backend.repository.DocumentoTicRepository;
import com.erwin.backend.repository.ReferenciaBibliograficaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** TAREA 1.5 / Art. 46: mínimo 10 referencias, 60% con antigüedad <= 5 años. */
@ExtendWith(MockitoExtension.class)
class BibliografiaServiceTest {

    @Mock private ReferenciaBibliograficaRepository referenciaRepo;
    @Mock private DocumentoTicRepository documentoRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private BibliografiaService service;

    @BeforeEach
    void setUp() {
        service = new BibliografiaService(referenciaRepo, documentoRepo, authorizationService);
    }

    private DocumentoTIC documento(EstandarBibliografico estandar) {
        Facultad facultad = new Facultad();
        facultad.setEstandarBibliografico(estandar);
        Carrera carrera = new Carrera();
        carrera.setFacultad(facultad);
        Estudiante estudiante = new Estudiante();
        estudiante.setCarrera(carrera);

        DocumentoTIC doc = new DocumentoTIC();
        doc.setId(1L);
        doc.setEstudiante(estudiante);
        return doc;
    }

    private ReferenciaBibliografica referencia(int anio) {
        ReferenciaBibliografica r = new ReferenciaBibliografica();
        r.setAutores("Pérez, J.");
        r.setTitulo("Un estudio");
        r.setAnioPublicacion(anio);
        r.setFuente("Revista X");
        r.setTipo(com.erwin.backend.enums.TipoReferenciaBibliografica.ARTICULO);
        return r;
    }

    @Test
    @DisplayName("formatear() indica que NO cumple el mínimo de 10 referencias")
    void indicaQueNoCumpleElMinimo() {
        when(documentoRepo.findById(1L)).thenReturn(Optional.of(documento(EstandarBibliografico.APA)));
        when(referenciaRepo.findByDocumento_Id(1L)).thenReturn(List.of(referencia(2024), referencia(2023)));

        BibliografiaFormateadaResponse resp = service.formatear(1L, null);

        assertThat(resp.total()).isEqualTo(2);
        assertThat(resp.minimoRequerido()).isEqualTo(10);
        assertThat(resp.cumpleMinimo()).isFalse();
    }

    @Test
    @DisplayName("formatear() calcula el 60% de referencias recientes (antigüedad <= 5 años) y detecta que SÍ cumple")
    void calculaPorcentajeDeRecientesYCumple() {
        when(documentoRepo.findById(1L)).thenReturn(Optional.of(documento(EstandarBibliografico.APA)));

        int anioActual = LocalDate.now().getYear();
        List<ReferenciaBibliografica> referencias = new ArrayList<>();
        for (int i = 0; i < 10; i++) referencias.add(referencia(anioActual)); // 10 recientes
        for (int i = 0; i < 2; i++) referencias.add(referencia(anioActual - 10)); // 2 antiguas
        // 10/12 = 83.3% recientes -> cumple 60%
        when(referenciaRepo.findByDocumento_Id(1L)).thenReturn(referencias);

        BibliografiaFormateadaResponse resp = service.formatear(1L, null);

        assertThat(resp.total()).isEqualTo(12);
        assertThat(resp.cumpleMinimo()).isTrue();
        assertThat(resp.porcentajeRecientes()).isGreaterThanOrEqualTo(60.0);
        assertThat(resp.cumpleRecientes()).isTrue();
    }

    @Test
    @DisplayName("formatear() detecta que NO cumple el 60% de referencias recientes")
    void detectaQueNoCumpleElPorcentajeDeRecientes() {
        when(documentoRepo.findById(1L)).thenReturn(Optional.of(documento(EstandarBibliografico.APA)));

        int anioActual = LocalDate.now().getYear();
        List<ReferenciaBibliografica> referencias = new ArrayList<>();
        for (int i = 0; i < 2; i++) referencias.add(referencia(anioActual)); // 2 recientes
        for (int i = 0; i < 10; i++) referencias.add(referencia(anioActual - 10)); // 10 antiguas
        // 2/12 = 16.6% recientes -> NO cumple 60%
        when(referenciaRepo.findByDocumento_Id(1L)).thenReturn(referencias);

        BibliografiaFormateadaResponse resp = service.formatear(1L, null);

        assertThat(resp.cumpleMinimo()).isTrue(); // 12 >= 10
        assertThat(resp.cumpleRecientes()).isFalse();
    }

    @Test
    @DisplayName("formatear() usa el estándar de la Facultad del estudiante cuando no se especifica uno")
    void usaElEstandarDeLaFacultadPorDefecto() {
        when(documentoRepo.findById(1L)).thenReturn(Optional.of(documento(EstandarBibliografico.IEEE)));
        when(referenciaRepo.findByDocumento_Id(1L)).thenReturn(List.of(referencia(2024)));

        BibliografiaFormateadaResponse resp = service.formatear(1L, null);

        assertThat(resp.estandar()).isEqualTo("IEEE");
        assertThat(resp.referenciasFormateadas().get(0)).startsWith("\"Un estudio\"");
    }

    @Test
    @DisplayName("crear() rechaza un año de publicación fuera de rango")
    void rechazaAnioDePublicacionInvalido() {
        when(documentoRepo.findById(1L)).thenReturn(Optional.of(documento(EstandarBibliografico.APA)));
        ReferenciaBibliograficaRequest req = new ReferenciaBibliograficaRequest(
                "Pérez, J.", "Título", 3000, "Fuente", "ARTICULO");

        assertThatThrownBy(() -> service.crear(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("año de publicación");
    }

    // ── validarMinimoYRecencia (FIX 4: bibliografía al crear la propuesta) ──

    private ReferenciaBibliograficaRequest req(int anio) {
        return new ReferenciaBibliograficaRequest("Pérez, J.", "Título", anio, "Fuente", "ARTICULO");
    }

    @Test
    @DisplayName("validarMinimoYRecencia() rechaza menos de 10 referencias con el conteo exacto en el mensaje")
    void validarMinimoYRecenciaRechazaMenosDeDiezReferencias() {
        int anioActual = LocalDate.now().getYear();
        List<ReferenciaBibliograficaRequest> referencias = List.of(req(anioActual), req(anioActual), req(anioActual));

        assertThatThrownBy(() -> service.validarMinimoYRecencia(referencias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mínimo 10")
                .hasMessageContaining("actualmente: 3");
    }

    @Test
    @DisplayName("validarMinimoYRecencia() rechaza 12 referencias con menos del 60% recientes")
    void validarMinimoYRecenciaRechazaPorcentajeInsuficiente() {
        int anioActual = LocalDate.now().getYear();
        List<ReferenciaBibliograficaRequest> referencias = new ArrayList<>();
        for (int i = 0; i < 4; i++) referencias.add(req(anioActual));       // recientes
        for (int i = 0; i < 8; i++) referencias.add(req(anioActual - 10)); // antiguas
        // 4/12 = 33.3% -> no cumple 60%

        assertThatThrownBy(() -> service.validarMinimoYRecencia(referencias))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60%");
    }

    @Test
    @DisplayName("validarMinimoYRecencia() acepta 12 referencias con 60% o más recientes")
    void validarMinimoYRecenciaAceptaCuandoCumpleAmbasReglas() {
        int anioActual = LocalDate.now().getYear();
        List<ReferenciaBibliograficaRequest> referencias = new ArrayList<>();
        for (int i = 0; i < 8; i++) referencias.add(req(anioActual));       // recientes
        for (int i = 0; i < 4; i++) referencias.add(req(anioActual - 10)); // antiguas
        // 8/12 = 66.6% -> cumple 60%

        assertThatCode(() -> service.validarMinimoYRecencia(referencias)).doesNotThrowAnyException();
    }
}
