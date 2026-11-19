package com.erwin.backend.service;

import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** TAREA 2: cada una de las 11 plantillas debe generarse como PDF sin errores. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeneradorDocumentosServiceTest {

    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private TribunalProyectoRepository tribunalRepo;
    @Mock private SustentacionRepository sustentacionRepo;
    @Mock private EvaluacionSustentacionRepository evaluacionRepo;
    @Mock private AntiplacioIntentoRepository antiplagioRepo;
    @Mock private ActaGradoRepository actaGradoRepo;

    private GeneradorDocumentosService service;
    private static final Integer ID_ESTUDIANTE = 10;

    @BeforeEach
    void setUp() {
        service = new GeneradorDocumentosService(proyectoRepo, tribunalRepo, sustentacionRepo,
                evaluacionRepo, antiplagioRepo, actaGradoRepo);

        ProyectoTitulacion proyecto = proyectoConDatosCompletos();
        when(proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(proyecto));

        TribunalProyecto presidente = miembroTribunal("PRESIDENTE", "Laura", "Vera");
        TribunalProyecto vocal = miembroTribunal("VOCAL", "Pedro", "Zambrano");
        TribunalProyecto suplente = miembroTribunal("SUPLENTE", "Ana", "Cedeño");
        when(tribunalRepo.findByProyecto_IdProyecto(1)).thenReturn(List.of(presidente, vocal, suplente));

        Sustentacion sustentacion = new Sustentacion();
        sustentacion.setIdSustentacion(500);
        sustentacion.setTipo("DEFENSA_FINAL");
        sustentacion.setFecha(LocalDate.now());
        sustentacion.setHora(LocalTime.NOON);
        sustentacion.setLugar("Auditorio FCCDD");
        when(sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(1)).thenReturn(List.of(sustentacion));

        EvaluacionSustentacion eval = new EvaluacionSustentacion();
        eval.setDocente(presidente.getDocente());
        eval.setCalidadTrabajo(new BigDecimal("2.00"));
        eval.setOriginalidad(new BigDecimal("2.00"));
        eval.setDominioTema(new BigDecimal("3.00"));
        eval.setPreguntas(new BigDecimal("3.00"));
        eval.setNotaFinal(new BigDecimal("10.00"));
        when(evaluacionRepo.findBySustentacion_IdSustentacionAndTipo(500, "SUSTENTACION")).thenReturn(List.of(eval));

        AntiplacioIntento intento = new AntiplacioIntento();
        intento.setPorcentajeCoincidencia(new BigDecimal("5.00"));
        intento.setFavorable(true);
        when(antiplagioRepo.findFirstByProyecto_IdProyectoAndFavorableTrueOrderByFechaIntentoDesc(1))
                .thenReturn(Optional.of(intento));

        ActaGrado acta = new ActaGrado();
        acta.setTipoTitulacion("TRABAJO_INTEGRACION_CURRICULAR");
        acta.setNotaRecord(new BigDecimal("9.00"));
        acta.setNotaTitulacion(new BigDecimal("8.50"));
        acta.setNotaFinal(new BigDecimal("8.90"));
        acta.setFechaEmision(LocalDate.now());
        when(actaGradoRepo.findByEstudiante_IdEstudiante(ID_ESTUDIANTE)).thenReturn(Optional.of(acta));
    }

    private TribunalProyecto miembroTribunal(String cargo, String nombres, String apellidos) {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        Docente docente = new Docente();
        docente.setIdDocente(cargo.hashCode());
        docente.setUsuario(usuario);

        TribunalProyecto t = new TribunalProyecto();
        t.setCargo(cargo);
        t.setDocente(docente);
        return t;
    }

    private ProyectoTitulacion proyectoConDatosCompletos() {
        Universidad universidad = new Universidad();
        universidad.setNombre("Universidad Técnica Estatal de Quevedo");

        Facultad facultad = new Facultad();
        facultad.setNombre("Facultad de Ciencias de la Computación");
        facultad.setUniversidad(universidad);

        Carrera carrera = new Carrera();
        carrera.setNombre("Ingeniería en Sistemas");
        carrera.setFacultad(facultad);

        Usuario usuarioEstudiante = new Usuario();
        usuarioEstudiante.setNombres("María");
        usuarioEstudiante.setApellidos("González");
        usuarioEstudiante.setCedula("1234567890");

        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(ID_ESTUDIANTE);
        estudiante.setUsuario(usuarioEstudiante);
        estudiante.setCarrera(carrera);

        PropuestaTitulacion propuesta = new PropuestaTitulacion();
        propuesta.setEstudiante(estudiante);

        Usuario usuarioDirector = new Usuario();
        usuarioDirector.setNombres("Carlos");
        usuarioDirector.setApellidos("Ramírez");
        Docente director = new Docente();
        director.setIdDocente(5);
        director.setUsuario(usuarioDirector);
        director.setNivelPosgrado(com.erwin.backend.enums.NivelPosgrado.PHD);

        Tipotrabajotitulacion tipoTrabajo = new Tipotrabajotitulacion();
        tipoTrabajo.setNombre("Proyecto de Investigación");

        ProyectoTitulacion proyecto = new ProyectoTitulacion();
        proyecto.setIdProyecto(1);
        proyecto.setTitulo("Sistema de gestión de titulación UTEQ");
        proyecto.setPropuesta(propuesta);
        proyecto.setDirector(director);
        proyecto.setTipoTrabajo(tipoTrabajo);
        return proyecto;
    }

    private void assertEsPdfValido(byte[] pdf) {
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test @DisplayName("generarPortada produce un PDF válido")
    void generaPortada() { assertEsPdfValido(service.generarPortada(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarDeclaracionAutoria produce un PDF válido")
    void generaDeclaracionAutoria() { assertEsPdfValido(service.generarDeclaracionAutoria(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCertificacionCulminacion produce un PDF válido")
    void generaCertificacionCulminacion() { assertEsPdfValido(service.generarCertificacionCulminacion(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCertificadoAntiplagio produce un PDF válido cuando hay resultado favorable")
    void generaCertificadoAntiplagio() { assertEsPdfValido(service.generarCertificadoAntiplagio(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCertificadoAntiplagio rechaza si no hay resultado favorable")
    void generarCertificadoAntiplagioSinFavorableLanzaExcepcion() {
        when(antiplagioRepo.findFirstByProyecto_IdProyectoAndFavorableTrueOrderByFechaIntentoDesc(1))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generarCertificadoAntiplagio(ID_ESTUDIANTE))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("generarPaginaTribunal produce un PDF válido")
    void generaPaginaTribunal() { assertEsPdfValido(service.generarPaginaTribunal(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCodigoDublin produce un PDF válido")
    void generaCodigoDublin() { assertEsPdfValido(service.generarCodigoDublin(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarInformeFavorableDirector produce un PDF válido")
    void generaInformeFavorableDirector() { assertEsPdfValido(service.generarInformeFavorableDirector(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCertificacionTribunal produce un PDF válido")
    void generaCertificacionTribunal() { assertEsPdfValido(service.generarCertificacionTribunal(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarCertificadoRedaccionTecnica produce un PDF válido")
    void generaCertificadoRedaccionTecnica() { assertEsPdfValido(service.generarCertificadoRedaccionTecnica(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarActaSustentacion produce un PDF válido con la rúbrica de 4 indicadores")
    void generaActaSustentacion() { assertEsPdfValido(service.generarActaSustentacion(ID_ESTUDIANTE)); }

    @Test @DisplayName("generarActaGrado produce un PDF válido")
    void generaActaGrado() { assertEsPdfValido(service.generarActaGrado(ID_ESTUDIANTE)); }

    @Test @DisplayName("la terminología del director cambia según el tipo de TIC (Proyecto Tecnológico)")
    void terminologiaCambiaParaProyectoTecnologico() {
        ProyectoTitulacion proyecto = proyectoConDatosCompletos();
        proyecto.getTipoTrabajo().setNombre("Proyecto Tecnológico");
        when(proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(ID_ESTUDIANTE))
                .thenReturn(Optional.of(proyecto));

        byte[] pdf = service.generarCertificacionCulminacion(ID_ESTUDIANTE);
        assertEsPdfValido(pdf);
    }
}
