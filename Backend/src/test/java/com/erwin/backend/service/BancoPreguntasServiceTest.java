package com.erwin.backend.service;

import com.erwin.backend.dtos.BancoPreguntasDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Validaciones del banco de preguntas: exactamente 1 opción correcta,
 * entre 2 y 5 opciones, y el conteo de preguntas activas usado para
 * habilitar el examen (>= 300 por carrera).
 */
@ExtendWith(MockitoExtension.class)
class BancoPreguntasServiceTest {

    @Mock private PreguntaComplexivoRepository preguntaRepo;
    @Mock private EjeTematicoRepository ejeRepo;
    @Mock private CarreraRepository carreraRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private BancoPreguntasService service;

    @BeforeEach
    void setUp() {
        service = new BancoPreguntasService(preguntaRepo, ejeRepo, carreraRepo, docenteRepo, periodoRepo, authorizationService);
    }

    private PreguntaComplexivoRequest requestConOpciones(List<OpcionRequest> opciones) {
        return new PreguntaComplexivoRequest(1L, 1, "¿Enunciado?", "BASICA", "EN_REVISION", "2025-2025", 5, opciones);
    }

    private void mockDependenciasBasicas() {
        EjeTematico eje = new EjeTematico();
        eje.setId(1L);
        when(ejeRepo.findById(1L)).thenReturn(Optional.of(eje));
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(1);
        when(carreraRepo.findById(1)).thenReturn(Optional.of(carrera));
        Docente docente = new Docente();
        docente.setIdDocente(5);
        when(docenteRepo.findById(5)).thenReturn(Optional.of(docente));
    }

    @Test
    @DisplayName("crear() rechaza si no hay exactamente 1 opción correcta")
    void rechazaSiNoHayExactamenteUnaOpcionCorrecta() {
        mockDependenciasBasicas();
        PreguntaComplexivoRequest req = requestConOpciones(List.of(
                new OpcionRequest(null, "A", true, 1),
                new OpcionRequest(null, "B", true, 2)
        ));

        assertThatThrownBy(() -> service.crear(req))
                .hasMessageContaining("exactamente 1 opción correcta");
    }

    @Test
    @DisplayName("crear() rechaza si no hay ninguna opción correcta")
    void rechazaSiNoHayOpcionCorrecta() {
        mockDependenciasBasicas();
        PreguntaComplexivoRequest req = requestConOpciones(List.of(
                new OpcionRequest(null, "A", false, 1),
                new OpcionRequest(null, "B", false, 2)
        ));

        assertThatThrownBy(() -> service.crear(req))
                .hasMessageContaining("exactamente 1 opción correcta");
    }

    @Test
    @DisplayName("crear() rechaza con menos de 2 opciones")
    void rechazaConMenosDeDosOpciones() {
        mockDependenciasBasicas();
        PreguntaComplexivoRequest req = requestConOpciones(List.of(
                new OpcionRequest(null, "A", true, 1)
        ));

        assertThatThrownBy(() -> service.crear(req))
                .hasMessageContaining("entre 2 y 5 opciones");
    }

    @Test
    @DisplayName("crear() rechaza con más de 5 opciones")
    void rechazaConMasDeCincoOpciones() {
        mockDependenciasBasicas();
        PreguntaComplexivoRequest req = requestConOpciones(List.of(
                new OpcionRequest(null, "A", true, 1),
                new OpcionRequest(null, "B", false, 2),
                new OpcionRequest(null, "C", false, 3),
                new OpcionRequest(null, "D", false, 4),
                new OpcionRequest(null, "E", false, 5),
                new OpcionRequest(null, "F", false, 6)
        ));

        assertThatThrownBy(() -> service.crear(req))
                .hasMessageContaining("entre 2 y 5 opciones");
    }

    @Test
    @DisplayName("crear() acepta una pregunta válida con 4 opciones y 1 correcta")
    void aceptaPreguntaValida() {
        mockDependenciasBasicas();
        when(preguntaRepo.save(any())).thenAnswer(inv -> {
            PreguntaComplexivo p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
        PreguntaComplexivoRequest req = requestConOpciones(List.of(
                new OpcionRequest(null, "A", false, 1),
                new OpcionRequest(null, "B", true, 2),
                new OpcionRequest(null, "C", false, 3),
                new OpcionRequest(null, "D", false, 4)
        ));

        PreguntaComplexivoResponse resp = service.crear(req);

        assertThat(resp.opciones()).hasSize(4);
        assertThat(resp.opciones().stream().filter(OpcionResponse::esCorrecta).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("estadisticas() indica que NO cumple el mínimo de 300 preguntas activas")
    void estadisticasIndicaSiNoCumpleElMinimo() {
        when(preguntaRepo.countByCarrera_IdCarreraAndEstado(1, com.erwin.backend.enums.EstadoPregunta.ACTIVA))
                .thenReturn(120L);
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of());

        EstadisticasBancoDto stats = service.estadisticas(1);

        assertThat(stats.totalActivas()).isEqualTo(120L);
        assertThat(stats.cumpleMinimo()).isFalse();
        assertThat(stats.minimoRequerido()).isEqualTo(300L);
    }
}
