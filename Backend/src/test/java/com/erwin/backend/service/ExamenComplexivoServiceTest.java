package com.erwin.backend.service;

import com.erwin.backend.dtos.ExamenComplexivoDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoEjeTematico;
import com.erwin.backend.enums.EstadoExamenComplexivo;
import com.erwin.backend.enums.EstadoPregunta;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Motor del Examen Complexivo real: selección aleatoria de 100 preguntas
 * distribuidas proporcionalmente por eje temático, temporizador de servidor
 * (timeout), cálculo de calificación, ajuste CONADIS y anulación por fraude.
 */
@ExtendWith(MockitoExtension.class)
class ExamenComplexivoServiceTest {

    @Mock private ExamenComplexivoAplicacionRepository examenRepo;
    @Mock private ExamenComplexivoRespuestaRepository respuestaRepo;
    @Mock private DelegadoExamenRepository delegadoRepo;
    @Mock private PreguntaComplexivoRepository preguntaRepo;
    @Mock private OpcionPreguntaRepository opcionRepo;
    @Mock private EjeTematicoRepository ejeRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private DocenteCarreraRepository docenteCarreraRepo;
    @Mock private MatriculaTitulacionRepository matriculaRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private OportunidadService oportunidadService;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private ExamenComplexivoService service;

    @BeforeEach
    void setUp() {
        service = new ExamenComplexivoService(
                examenRepo, respuestaRepo, delegadoRepo, preguntaRepo, opcionRepo, ejeRepo,
                estudianteRepo, docenteRepo, docenteCarreraRepo, matriculaRepo, periodoRepo, oportunidadService,
                authorizationService);
    }

    private Carrera carrera(int id) {
        Carrera c = new Carrera();
        c.setIdCarrera(id);
        c.setNombre("Software");
        return c;
    }

    private Estudiante estudiante(int id, Carrera carrera) {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(id);
        e.setCarrera(carrera);
        Usuario u = new Usuario();
        u.setNombres("Ana");
        u.setApellidos("Torres");
        e.setUsuario(u);
        return e;
    }

    private EjeTematico eje(long id, Carrera carrera) {
        EjeTematico eje = new EjeTematico();
        eje.setId(id);
        eje.setCarrera(carrera);
        eje.setNombre("Eje " + id);
        eje.setEstado(EstadoEjeTematico.ACTIVO);
        return eje;
    }

    private List<PreguntaComplexivo> preguntasDelEje(EjeTematico eje, int cantidad) {
        List<PreguntaComplexivo> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            PreguntaComplexivo p = new PreguntaComplexivo();
            p.setId((long) (eje.getId() * 1000 + i));
            p.setEjeTematico(eje);
            p.setEnunciado("Pregunta " + i);
            p.setEstado(EstadoPregunta.ACTIVA);
            p.setOpciones(new ArrayList<>());
            lista.add(p);
        }
        return lista;
    }

    private ExamenComplexivoAplicacion examenListoParaIniciar(Estudiante estudiante) {
        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setId(500L);
        examen.setEstudiante(estudiante);
        examen.setEstado(EstadoExamenComplexivo.PROGRAMADO);
        examen.setFirmadoCompromisosEticos(true);
        return examen;
    }

    @Test
    @DisplayName("iniciar() selecciona exactamente 100 preguntas y crea 100 respuestas vacías")
    void iniciarSeleccionaCienPreguntas() {
        Carrera carrera = carrera(1);
        Estudiante estudiante = estudiante(10, carrera);
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);

        EjeTematico eje1 = eje(1, carrera), eje2 = eje(2, carrera), eje3 = eje(3, carrera);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of(eje1, eje2, eje3));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(1L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje1, 40));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(2L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje2, 40));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(3L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje3, 40));
        when(respuestaRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IniciarExamenResponse resp = service.iniciar(500L);

        assertThat(resp.preguntas()).hasSize(100);
        assertThat(examen.getEstado()).isEqualTo(EstadoExamenComplexivo.EN_CURSO);
    }

    @Test
    @DisplayName("iniciar() distribuye las 100 preguntas proporcionalmente entre los 3 ejes (34/33/33)")
    void iniciarDistribuyeProporcionalmentePorEje() {
        Carrera carrera = carrera(1);
        Estudiante estudiante = estudiante(10, carrera);
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);

        EjeTematico eje1 = eje(1, carrera), eje2 = eje(2, carrera), eje3 = eje(3, carrera);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of(eje1, eje2, eje3));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(1L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje1, 40));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(2L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje2, 40));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(3L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje3, 40));
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<ExamenComplexivoRespuesta>> captor = ArgumentCaptor.forClass(List.class);
        when(respuestaRepo.saveAll(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciar(500L);

        List<ExamenComplexivoRespuesta> respuestas = captor.getValue();
        long delEje1 = respuestas.stream().filter(r -> r.getPregunta().getEjeTematico().getId() == 1L).count();
        long delEje2 = respuestas.stream().filter(r -> r.getPregunta().getEjeTematico().getId() == 2L).count();
        long delEje3 = respuestas.stream().filter(r -> r.getPregunta().getEjeTematico().getId() == 3L).count();

        assertThat(delEje1 + delEje2 + delEje3).isEqualTo(100);
        assertThat(List.of(delEje1, delEje2, delEje3)).allMatch(c -> c == 33 || c == 34);
    }

    @Test
    @DisplayName("iniciar() rechaza si un eje no tiene suficientes preguntas activas")
    void iniciarRechazaSiFaltanPreguntasEnUnEje() {
        Carrera carrera = carrera(1);
        Estudiante estudiante = estudiante(10, carrera);
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);

        EjeTematico eje1 = eje(1, carrera), eje2 = eje(2, carrera), eje3 = eje(3, carrera);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of(eje1, eje2, eje3));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(1L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje1, 5));

        assertThatThrownBy(() -> service.iniciar(500L))
                .hasMessageContaining("no tiene suficientes preguntas activas");
        verify(respuestaRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("iniciar() ajusta la duración a 240 minutos si el estudiante tiene CONADIS >= 60%")
    void iniciarAjustaDuracionParaConadis() {
        Carrera carrera = carrera(1);
        Estudiante estudiante = estudiante(10, carrera);
        estudiante.setDiscapacidad(true);
        estudiante.setPorcentajeDiscapacidad(60);
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);

        EjeTematico eje1 = eje(1, carrera);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of(eje1));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(1L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje1, 150));
        when(respuestaRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciar(500L);

        assertThat(examen.getDuracionMinutos()).isEqualTo(240);
        assertThat(examen.getHoraFin()).isEqualTo(examen.getHoraInicio().plusMinutes(240));
    }

    @Test
    @DisplayName("iniciar() con estudiante sin CONADIS usa la duración normal de 180 minutos")
    void iniciarUsaDuracionNormalSinConadis() {
        Carrera carrera = carrera(1);
        Estudiante estudiante = estudiante(10, carrera);
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);

        EjeTematico eje1 = eje(1, carrera);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));
        when(ejeRepo.findByCarrera_IdCarrera(1)).thenReturn(List.of(eje1));
        when(preguntaRepo.findByEjeTematico_IdAndEstado(1L, EstadoPregunta.ACTIVA)).thenReturn(preguntasDelEje(eje1, 150));
        when(respuestaRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciar(500L);

        assertThat(examen.getDuracionMinutos()).isEqualTo(180);
    }

    @Test
    @DisplayName("iniciar() rechaza si no se firmaron los compromisos éticos en el acta de ingreso")
    void iniciarRechazaSinCompromisosEticos() {
        Estudiante estudiante = estudiante(10, carrera(1));
        ExamenComplexivoAplicacion examen = examenListoParaIniciar(estudiante);
        examen.setFirmadoCompromisosEticos(false);
        when(examenRepo.findById(500L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.iniciar(500L))
                .hasMessageContaining("compromisos éticos");
    }

    // ── Cálculo de calificación ──────────────────────────────────────────
    @Test
    @DisplayName("finalizar() calcula la calificación como respuestas correctas x 0.1")
    void finalizarCalculaCalificacionPorRespuestasCorrectas() {
        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setId(700L);
        examen.setEstado(EstadoExamenComplexivo.EN_CURSO);
        examen.setTipo(com.erwin.backend.enums.TipoExamenComplexivo.FINAL);
        examen.setHoraFin(LocalDateTime.now().plusMinutes(30));
        when(examenRepo.findById(700L)).thenReturn(Optional.of(examen));

        List<ExamenComplexivoRespuesta> respuestas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ExamenComplexivoRespuesta r = new ExamenComplexivoRespuesta();
            if (i < 7) {
                OpcionPregunta correcta = new OpcionPregunta();
                correcta.setEsCorrecta(true);
                r.setOpcionSeleccionada(correcta);
            } else if (i < 9) {
                OpcionPregunta incorrecta = new OpcionPregunta();
                incorrecta.setEsCorrecta(false);
                r.setOpcionSeleccionada(incorrecta);
            } // última sin responder: opcionSeleccionada = null
            respuestas.add(r);
        }
        when(respuestaRepo.findByExamen_IdOrderByOrden(700L)).thenReturn(respuestas);
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultadoExamenDto resultado = service.finalizar(700L);

        assertThat(resultado.totalPreguntasCorrectas()).isEqualTo(7);
        assertThat(resultado.calificacion()).isEqualByComparingTo(new BigDecimal("0.70"));
        assertThat(examen.getEstado()).isEqualTo(EstadoExamenComplexivo.FINALIZADO);
    }

    // ── Timeout de servidor ───────────────────────────────────────────────
    @Test
    @DisplayName("el scheduler de timeout finaliza automáticamente los exámenes EN_CURSO vencidos")
    void timeoutFinalizaExamenesVencidosAutomaticamente() {
        ExamenComplexivoAplicacion vencido = new ExamenComplexivoAplicacion();
        vencido.setId(900L);
        vencido.setEstado(EstadoExamenComplexivo.EN_CURSO);
        vencido.setTipo(com.erwin.backend.enums.TipoExamenComplexivo.FINAL);
        vencido.setHoraFin(LocalDateTime.now().minusMinutes(5));

        when(examenRepo.findByEstadoAndHoraFinBefore(eq(EstadoExamenComplexivo.EN_CURSO), any()))
                .thenReturn(List.of(vencido));
        when(respuestaRepo.findByExamen_IdOrderByOrden(900L)).thenReturn(List.of());
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Long> finalizados = service.finalizarExamenesVencidos();

        assertThat(finalizados).containsExactly(900L);
        assertThat(vencido.getEstado()).isEqualTo(EstadoExamenComplexivo.FINALIZADO);
        assertThat(vencido.getCalificacion()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("responder() rechaza y auto-finaliza si el tiempo del examen ya venció")
    void responderRechazaYFinalizaSiVencioElTiempo() {
        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setId(950L);
        examen.setEstado(EstadoExamenComplexivo.EN_CURSO);
        examen.setTipo(com.erwin.backend.enums.TipoExamenComplexivo.FINAL);
        examen.setHoraFin(LocalDateTime.now().minusSeconds(1));
        when(examenRepo.findById(950L)).thenReturn(Optional.of(examen));
        when(respuestaRepo.findByExamen_IdOrderByOrden(950L)).thenReturn(List.of());
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.responder(950L, new ResponderPreguntaRequest(1L, 2L)))
                .hasMessageContaining("vencido");

        assertThat(examen.getEstado()).isEqualTo(EstadoExamenComplexivo.FINALIZADO);
    }

    // ── Fraude académico ──────────────────────────────────────────────────
    @Test
    @DisplayName("registrarFraude() anula el examen, califica 0 y registra el incidente")
    void registrarFraudeAnulaElExamen() {
        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setId(800L);
        examen.setEstado(EstadoExamenComplexivo.EN_CURSO);
        examen.setTipo(com.erwin.backend.enums.TipoExamenComplexivo.FINAL);
        when(examenRepo.findById(800L)).thenReturn(Optional.of(examen));
        when(examenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrarFraudeRequest req = new RegistrarFraudeRequest("Uso de celular durante el examen", "foto_evidencia_1");
        ExamenAplicacionResponse resp = service.registrarFraude(800L, req);

        assertThat(resp.estado()).isEqualTo("ANULADO");
        assertThat(resp.calificacion()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.observaciones()).contains("Uso de celular durante el examen");
        assertThat(examen.getEstado()).isEqualTo(EstadoExamenComplexivo.ANULADO);
    }

    @Test
    @DisplayName("registrarFraude() rechaza sobre un examen ya finalizado")
    void registrarFraudeRechazaSiYaFinalizado() {
        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setId(801L);
        examen.setEstado(EstadoExamenComplexivo.FINALIZADO);
        when(examenRepo.findById(801L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.registrarFraude(801L,
                new RegistrarFraudeRequest("desc", "ev")))
                .hasMessageContaining("FINALIZADO");
        verify(examenRepo, never()).save(any());
    }
}
