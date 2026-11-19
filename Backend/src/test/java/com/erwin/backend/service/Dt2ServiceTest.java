package com.erwin.backend.service;

import com.erwin.backend.dtos.Dt2Dtos;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de calificación de Titulación II (Dt2Service):
 * ponderación 60/40 de predefensa, umbral de aprobación 7/10, rúbrica
 * 20/20/30/30 de sustentación final, y nota de grado 80/20.
 */
@ExtendWith(MockitoExtension.class)
class Dt2ServiceTest {

    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private AnteproyectoTitulacionRepository anteproyectoRepo;
    @Mock private DocumentoTitulacionRepository documentoRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private Dt2AsignacionRepository dt2AsignacionRepo;
    @Mock private BitacoraAsignacionRepository bitacoraRepo;
    @Mock private TribunalProyectoRepository tribunalRepo;
    @Mock private AsesoriaDirectorRepository asesoriaRepo;
    @Mock private ActaCorteRepository actaCorteRepo;
    @Mock private AntiplacioIntentoRepository antiplacioRepo;
    @Mock private SustentacionRepository sustentacionRepo;
    @Mock private EvaluacionSustentacionRepository evaluacionRepo;
    @Mock private DocumentoPrevioSustentacionRepository docPrevioRepo;
    @Mock private ActaGradoRepository actaGradoRepo;
    @Mock private DocumentStorageService storageService;
    @Mock private EmailService emailService;
    @Mock private OportunidadService oportunidadService;

    private Dt2Service dt2Service;

    @BeforeEach
    void setUp() {
        dt2Service = new Dt2Service(
                proyectoRepo, anteproyectoRepo, documentoRepo, docenteRepo, usuarioRepo,
                dt2AsignacionRepo, bitacoraRepo, tribunalRepo, asesoriaRepo, actaCorteRepo,
                antiplacioRepo, sustentacionRepo, evaluacionRepo, docPrevioRepo, actaGradoRepo,
                storageService, emailService, oportunidadService
        );
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Estudiante crearEstudiante(int idEstudiante, BigDecimal promedioRecord80) {
        Usuario usuario = new Usuario();
        usuario.setNombres("Ana");
        usuario.setApellidos("Pérez");

        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(idEstudiante);
        estudiante.setUsuario(usuario);
        estudiante.setPromedioRecord80(promedioRecord80);
        return estudiante;
    }

    private ProyectoTitulacion crearProyecto(Integer idProyecto, Estudiante estudiante) {
        PropuestaTitulacion propuesta = new PropuestaTitulacion();
        propuesta.setEstudiante(estudiante);

        ProyectoTitulacion proyecto = new ProyectoTitulacion();
        proyecto.setIdProyecto(idProyecto);
        proyecto.setTitulo("Sistema de Titulación UTEQ");
        proyecto.setPropuesta(propuesta);
        proyecto.setEstado("SUSTENTACION");
        return proyecto;
    }

    private EvaluacionSustentacion evaluacionConNota(BigDecimal nota) {
        EvaluacionSustentacion eval = new EvaluacionSustentacion();
        eval.setNotaFinal(nota);
        return eval;
    }

    // ── Test 1: Ponderación 60/40 ────────────────────────────────────────────

    @Test
    @DisplayName("La nota ponderada de predefensa es notaDocente*0.60 + promedioTribunal*0.40")
    void laNotaPonderadaDePredefensaAplicaSesentaCuarenta() {
        Integer idProyecto = 1;
        Estudiante estudiante = crearEstudiante(10, new BigDecimal("9.00"));
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, estudiante);

        Sustentacion predefensa = new Sustentacion();
        predefensa.setIdSustentacion(100);
        predefensa.setTipo("PREDEFENSA");
        predefensa.setFecha(LocalDate.now());
        predefensa.setHora(LocalTime.NOON);
        predefensa.setLugar("Aula 101");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(predefensa));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(100, "PREDEFENSA_DOCENTE"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("8.00"))));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(100, "PREDEFENSA_TRIBUNAL"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("7.00")), evaluacionConNota(new BigDecimal("9.00"))));
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(2L);

        Dt2Dtos.PredefensaDto dto = dt2Service.getPredefensaDto(idProyecto);

        // notaDocente = 8.00 ; promedioTribunal = (7.00+9.00)/2 = 8.00
        // ponderada = 8.00*0.60 + 8.00*0.40 = 8.00
        assertThat(dto.getPromedioTribunal()).isEqualByComparingTo("8.00");
        assertThat(dto.getNotaFinalPonderada()).isEqualByComparingTo("8.00");
    }

    // ── Test 2: Umbral de aprobación 7/10 ────────────────────────────────────

    @Test
    @DisplayName("Predefensa con nota ponderada >= 7.0 queda APROBADA")
    void predefensaConNotaMayorOIgualASieteQuedaAprobada() {
        Integer idProyecto = 2;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(11, new BigDecimal("8.00")));

        Sustentacion predefensa = new Sustentacion();
        predefensa.setIdSustentacion(200);
        predefensa.setTipo("PREDEFENSA");
        predefensa.setFecha(LocalDate.now());
        predefensa.setHora(LocalTime.NOON);
        predefensa.setLugar("Aula 101");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(predefensa));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(200, "PREDEFENSA_DOCENTE"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("7.00"))));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(200, "PREDEFENSA_TRIBUNAL"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("7.00"))));
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(1L);

        Dt2Dtos.PredefensaDto dto = dt2Service.getPredefensaDto(idProyecto);

        assertThat(dto.getNotaFinalPonderada()).isEqualByComparingTo("7.00");
        assertThat(dto.getEstado()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("Predefensa con nota ponderada < 7.0 NO queda aprobada")
    void predefensaConNotaMenorASieteNoQuedaAprobada() {
        Integer idProyecto = 3;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(12, new BigDecimal("8.00")));

        Sustentacion predefensa = new Sustentacion();
        predefensa.setIdSustentacion(300);
        predefensa.setTipo("PREDEFENSA");
        predefensa.setFecha(LocalDate.now());
        predefensa.setHora(LocalTime.NOON);
        predefensa.setLugar("Aula 101");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(predefensa));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(300, "PREDEFENSA_DOCENTE"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("6.00"))));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(300, "PREDEFENSA_TRIBUNAL"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("6.50"))));
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(1L);

        Dt2Dtos.PredefensaDto dto = dt2Service.getPredefensaDto(idProyecto);

        assertThat(dto.getNotaFinalPonderada()).isLessThan(new BigDecimal("7.00"));
        assertThat(dto.getEstado()).isNotEqualTo("APROBADA");
    }

    // ── Test 3: Rúbrica 20/20/30/30 ──────────────────────────────────────────

    @Test
    @DisplayName("La nota de sustentación aplica calidad*0.20 + originalidad*0.20 + dominio*0.30 + preguntas*0.30")
    void laNotaDeSustentacionAplicaLaRubricaVeinteVeinteTreintaTreinta() {
        Integer idProyecto = 4;
        Integer idDocente = 50;

        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(13, new BigDecimal("9.00")));

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.LISTO_SUSTENTACION);

        Sustentacion defensaFinal = new Sustentacion();
        defensaFinal.setIdSustentacion(400);
        defensaFinal.setTipo("DEFENSA_FINAL");
        defensaFinal.setFecha(LocalDate.now());
        defensaFinal.setHora(LocalTime.NOON);
        defensaFinal.setLugar("Auditorio");

        Docente docente = new Docente();
        docente.setIdDocente(idDocente);

        Dt2Dtos.CalificarSustentacionRequest req = new Dt2Dtos.CalificarSustentacionRequest(
                idProyecto, idDocente,
                new BigDecimal("8.00"),  // calidad * 0.20
                new BigDecimal("9.00"),  // originalidad * 0.20
                new BigDecimal("7.00"),  // dominio * 0.30
                new BigDecimal("10.00"), // preguntas * 0.30
                "Sustentación sólida"
        );

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(idProyecto, idDocente)).thenReturn(true);
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(defensaFinal));
        when(docenteRepo.findById(idDocente)).thenReturn(Optional.of(docente));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(400, idDocente, "SUSTENTACION"))
                .thenReturn(Optional.empty());
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(400, "SUSTENTACION"))
                .thenReturn(List.of());

        dt2Service.calificarSustentacion(req);

        ArgumentCaptor<EvaluacionSustentacion> captor = ArgumentCaptor.forClass(EvaluacionSustentacion.class);
        org.mockito.Mockito.verify(evaluacionRepo).save(captor.capture());

        // 8.00*0.20 + 9.00*0.20 + 7.00*0.30 + 10.00*0.30 = 1.60 + 1.80 + 2.10 + 3.00 = 8.50
        assertThat(captor.getValue().getNotaFinal()).isEqualByComparingTo("8.50");

        // Art. 65: la calificación queda inapelable en el momento en que se emite,
        // no solo cuando se consolida el tribunal completo (ver validarNoFinalizada).
        assertThat(captor.getValue().getCalificacionFinalizada()).isTrue();
    }

    @Test
    @DisplayName("Art. 65: un miembro del tribunal no puede recalificar una sustentación ya emitida")
    void unMiembroDelTribunalNoPuedeRecalificarUnaSustentacionYaEmitida() {
        Integer idProyecto = 41;
        Integer idDocente = 51;

        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(16, new BigDecimal("9.00")));

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.LISTO_SUSTENTACION);

        Sustentacion defensaFinal = new Sustentacion();
        defensaFinal.setIdSustentacion(401);
        defensaFinal.setTipo("DEFENSA_FINAL");
        defensaFinal.setFecha(LocalDate.now());
        defensaFinal.setHora(LocalTime.NOON);
        defensaFinal.setLugar("Auditorio");

        Dt2Dtos.CalificarSustentacionRequest req = new Dt2Dtos.CalificarSustentacionRequest(
                idProyecto, idDocente,
                new BigDecimal("8.00"), new BigDecimal("8.00"),
                new BigDecimal("8.00"), new BigDecimal("8.00"),
                "Segundo intento de calificar"
        );

        Docente docente = new Docente();
        docente.setIdDocente(idDocente);

        EvaluacionSustentacion evalYaFinalizada = new EvaluacionSustentacion();
        evalYaFinalizada.setCalificacionFinalizada(true);

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(idProyecto, idDocente)).thenReturn(true);
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(defensaFinal));
        when(docenteRepo.findById(idDocente)).thenReturn(Optional.of(docente));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(401, idDocente, "SUSTENTACION"))
                .thenReturn(Optional.of(evalYaFinalizada));

        assertThatThrownBy(() -> dt2Service.calificarSustentacion(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("inapelable");
    }

    // ── Test 5: Nota de grado 80/20 ──────────────────────────────────────────

    @Test
    @DisplayName("La nota de grado es promedioRecord*0.80 + notaSustentacion*0.20")
    void laNotaDeGradoAplicaOchentaVeinte() {
        Integer idProyecto = 5;
        Estudiante estudiante = crearEstudiante(14, new BigDecimal("9.00"));
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, estudiante);

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.LISTO_SUSTENTACION);

        Sustentacion defensaFinal = new Sustentacion();
        defensaFinal.setIdSustentacion(500);
        defensaFinal.setTipo("DEFENSA_FINAL");
        defensaFinal.setFecha(LocalDate.now());
        defensaFinal.setHora(LocalTime.NOON);
        defensaFinal.setLugar("Auditorio");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(defensaFinal));
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(1L);
        when(evaluacionRepo.countBySustentacion_IdSustentacionAndTipo(500, "SUSTENTACION")).thenReturn(1L);
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(500, "SUSTENTACION"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("8.00"))));
        when(actaGradoRepo.findByEstudiante_IdEstudiante(14)).thenReturn(Optional.empty());

        Dt2Dtos.ResultadoSustentacionDto resultado = dt2Service.consolidarResultado(idProyecto);

        // promedioRecord80 = 9.00 ; notaSustentacion (promedioTribunal) = 8.00
        // notaGrado = 9.00*0.80 + 8.00*0.20 = 7.20 + 1.60 = 8.80
        assertThat(resultado.getNotaGradoFinal()).isEqualByComparingTo("8.80");
    }

    // ── Test 6: Nota de grado con promedioRecord null → excepción ───────────

    @Test
    @DisplayName("Consolidar resultado con promedioRecord80 null lanza IllegalStateException")
    void consolidarResultadoConPromedioRecordNuloLanzaExcepcion() {
        Integer idProyecto = 6;
        Estudiante estudiante = crearEstudiante(15, null); // sin promedio de récord académico
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, estudiante);

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.LISTO_SUSTENTACION);

        Sustentacion defensaFinal = new Sustentacion();
        defensaFinal.setIdSustentacion(600);
        defensaFinal.setTipo("DEFENSA_FINAL");
        defensaFinal.setFecha(LocalDate.now());
        defensaFinal.setHora(LocalTime.NOON);
        defensaFinal.setLugar("Auditorio");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(defensaFinal));
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(1L);
        when(evaluacionRepo.countBySustentacion_IdSustentacionAndTipo(600, "SUSTENTACION")).thenReturn(1L);
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(600, "SUSTENTACION"))
                .thenReturn(List.of(evaluacionConNota(new BigDecimal("8.00"))));

        assertThatThrownBy(() -> dt2Service.consolidarResultado(idProyecto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("15");
    }

    // ── Test 7: Bloqueo de cierre de corte sin 5 asesorías ───────────────────

    @Test
    @DisplayName("cerrarCorte rechaza el cierre si hay menos de 5 asesorías registradas")
    void cerrarCorteRechazaSiFaltanAsesorias() {
        Integer idProyecto = 7;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(16, new BigDecimal("8.00")));
        proyecto.setEstado("DESARROLLO");

        Dt2Dtos.CerrarCorteRequest req = new Dt2Dtos.CerrarCorteRequest(
                idProyecto, 90, 1, new BigDecimal("8.00"), "Corte 1");

        Docente director = new Docente();
        director.setIdDocente(90);

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(actaCorteRepo.existsByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1)).thenReturn(false);
        when(docenteRepo.findById(90)).thenReturn(Optional.of(director));
        when(asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1)).thenReturn(3L);

        assertThatThrownBy(() -> dt2Service.cerrarCorte(req))
                .hasMessageContaining("mínimo 5 asesorías")
                .hasMessageContaining("actualmente: 3");

        org.mockito.Mockito.verify(actaCorteRepo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("cerrarCorte permite el cierre con 5 o más asesorías registradas")
    void cerrarCortePermiteElCierreConCincoAsesorias() {
        Integer idProyecto = 8;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(17, new BigDecimal("8.00")));
        proyecto.setEstado("DESARROLLO");

        Docente director = new Docente();
        director.setIdDocente(90);

        Dt2Dtos.CerrarCorteRequest req = new Dt2Dtos.CerrarCorteRequest(
                idProyecto, 90, 1, new BigDecimal("8.00"), "Corte 1");

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(actaCorteRepo.existsByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1)).thenReturn(false);
        when(asesoriaRepo.countByProyecto_IdProyectoAndNumeroCorte(idProyecto, 1)).thenReturn(5L);
        when(docenteRepo.findById(90)).thenReturn(Optional.of(director));
        when(actaCorteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Dt2Dtos.ActaCorteDto dto = dt2Service.cerrarCorte(req);

        assertThat(dto.getTotalAsesorias()).isEqualTo(5);
        assertThat(dto.getAsesoriasSuficientes()).isTrue();
    }

    // ── Test 8: Rúbrica 20/20/30/30 en predefensa ────────────────────────────

    @Test
    @DisplayName("La nota de predefensa del tribunal aplica la rúbrica 20/20/30/30")
    void laNotaDePredefensaDelTribunalAplicaLaRubrica() {
        Integer idProyecto = 9;
        Integer idDocente = 60;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(18, new BigDecimal("9.00")));

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.EN_PREDEFENSA);

        Sustentacion predefensa = new Sustentacion();
        predefensa.setIdSustentacion(700);
        predefensa.setTipo("PREDEFENSA");
        predefensa.setFecha(LocalDate.now());

        Docente docente = new Docente();
        docente.setIdDocente(idDocente);

        Dt2Dtos.CalificarPredefensaTribunalRequest req = new Dt2Dtos.CalificarPredefensaTribunalRequest(
                idProyecto, idDocente,
                new BigDecimal("8.00"),  // calidadTrabajo * 0.20
                new BigDecimal("9.00"),  // capacidadCreadora * 0.20
                new BigDecimal("7.00"),  // dominioExposicion * 0.30
                new BigDecimal("10.00"), // pertinenciaRespuestas * 0.30
                "Buena defensa", false
        );

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(idProyecto, idDocente)).thenReturn(true);
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(predefensa));
        when(docenteRepo.findById(idDocente)).thenReturn(Optional.of(docente));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(700, idDocente, "PREDEFENSA_TRIBUNAL"))
                .thenReturn(Optional.empty());
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(700, "PREDEFENSA_DOCENTE")).thenReturn(List.of());
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(700, "PREDEFENSA_TRIBUNAL")).thenReturn(List.of());
        when(tribunalRepo.countByProyecto_IdProyecto(idProyecto)).thenReturn(1L);

        dt2Service.calificarPredefensaTribunal(req);

        ArgumentCaptor<EvaluacionSustentacion> captor = ArgumentCaptor.forClass(EvaluacionSustentacion.class);
        org.mockito.Mockito.verify(evaluacionRepo).save(captor.capture());

        // 8.00*0.20 + 9.00*0.20 + 7.00*0.30 + 10.00*0.30 = 8.50
        assertThat(captor.getValue().getNotaFinal()).isEqualByComparingTo("8.50");
    }

    // ── Test 9: Ventana de 15 días para segunda sustentación ─────────────────

    @Test
    @DisplayName("habilitarSegundaOportunidad rechaza una fecha fuera de los 15 días posteriores a la reprobada")
    void habilitarSegundaOportunidadRechazaFechaFueraDePlazo() {
        Integer idProyecto = 10;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(19, new BigDecimal("8.00")));
        proyecto.setEstado("REPROBADO");

        Sustentacion primeraReprobada = new Sustentacion();
        primeraReprobada.setIdSustentacion(800);
        primeraReprobada.setTipo("DEFENSA_FINAL");
        primeraReprobada.setFecha(LocalDate.of(2026, 1, 1));

        Dt2Dtos.SegundaOportunidadRequest req = new Dt2Dtos.SegundaOportunidadRequest(
                idProyecto, 1, LocalDate.of(2026, 1, 20), LocalTime.NOON, "Aula 5", null);

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(primeraReprobada));

        assertThatThrownBy(() -> dt2Service.habilitarSegundaOportunidad(req))
                .hasMessageContaining("15 días")
                .hasMessageContaining("Art. 12");

        org.mockito.Mockito.verify(sustentacionRepo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("habilitarSegundaOportunidad permite una fecha dentro de los 15 días posteriores")
    void habilitarSegundaOportunidadPermiteFechaDentroDePlazo() {
        Integer idProyecto = 11;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(20, new BigDecimal("8.00")));
        proyecto.setEstado("REPROBADO");

        Sustentacion primeraReprobada = new Sustentacion();
        primeraReprobada.setIdSustentacion(900);
        primeraReprobada.setTipo("DEFENSA_FINAL");
        primeraReprobada.setFecha(LocalDate.of(2026, 1, 1));

        Dt2Dtos.SegundaOportunidadRequest req = new Dt2Dtos.SegundaOportunidadRequest(
                idProyecto, 1, LocalDate.of(2026, 1, 10), LocalTime.NOON, "Aula 5", null);

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(primeraReprobada));
        when(sustentacionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Dt2Dtos.MensajeDto resultado = dt2Service.habilitarSegundaOportunidad(req);

        assertThat(resultado.getExito()).isTrue();
    }

    // ── Test 10: Calificación inapelable (Art. 65) ───────────────────────────

    @Test
    @DisplayName("No se puede modificar una calificación de sustentación ya finalizada")
    void noSePuedeModificarUnaCalificacionFinalizada() {
        Integer idProyecto = 12;
        Integer idDocente = 70;
        ProyectoTitulacion proyecto = crearProyecto(idProyecto, crearEstudiante(21, new BigDecimal("9.00")));

        DocumentoTitulacion documento = new DocumentoTitulacion();
        documento.setEstado(com.erwin.backend.enums.EstadoDocumento.LISTO_SUSTENTACION);

        Sustentacion defensaFinal = new Sustentacion();
        defensaFinal.setIdSustentacion(1000);
        defensaFinal.setTipo("DEFENSA_FINAL");
        defensaFinal.setFecha(LocalDate.now());

        EvaluacionSustentacion existente = evaluacionConNota(new BigDecimal("8.00"));
        existente.setCalificacionFinalizada(true);

        Dt2Dtos.CalificarSustentacionRequest req = new Dt2Dtos.CalificarSustentacionRequest(
                idProyecto, idDocente,
                new BigDecimal("9.00"), new BigDecimal("9.00"), new BigDecimal("9.00"), new BigDecimal("9.00"),
                "Intento de modificar");

        Docente docente = new Docente();
        docente.setIdDocente(idDocente);

        when(proyectoRepo.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(documentoRepo.findByProyecto_IdProyecto(idProyecto)).thenReturn(Optional.of(documento));
        when(tribunalRepo.existsByProyecto_IdProyectoAndDocente_IdDocente(idProyecto, idDocente)).thenReturn(true);
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto))
                .thenReturn(List.of(defensaFinal));
        when(docenteRepo.findById(idDocente)).thenReturn(Optional.of(docente));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndDocente_IdDocenteAndTipo(1000, idDocente, "SUSTENTACION"))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> dt2Service.calificarSustentacion(req))
                .hasMessageContaining("inapelable")
                .hasMessageContaining("Art. 65");

        org.mockito.Mockito.verify(evaluacionRepo, org.mockito.Mockito.never()).save(any());
    }
}
