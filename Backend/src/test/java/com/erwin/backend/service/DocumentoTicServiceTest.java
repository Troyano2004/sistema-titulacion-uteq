package com.erwin.backend.service;

import com.erwin.backend.dtos.DocumentoTicDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.NivelPosgrado;
import com.erwin.backend.enums.TipoTicPlantilla;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** TAREA 1.2 (generación de secciones) y 1.3 (auto-llenado de portada). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoTicServiceTest {

    @Mock private DocumentoTicRepository documentoRepo;
    @Mock private SeccionDocumentoTicRepository seccionRepo;
    @Mock private VersionDocumentoTicRepository versionRepo;
    @Mock private ComentarioDocumentoTicRepository comentarioRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private TipoTrabajoTitulacionRepository tipoTrabajoRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;
    @Mock private SeccionPlantillaTicService seccionPlantillaTicService;

    private DocumentoTicService service;
    private final List<SeccionDocumentoTIC> almacenadas = new ArrayList<>();

    /** Misma estructura de PROYECTO_INVESTIGACION que la migración V18 (Art. 42). */
    private static final List<SeccionPlantillaProvider.SeccionDef> PLANTILLA_PROYECTO_INVESTIGACION = List.of(
            new SeccionPlantillaProvider.SeccionDef("PORTADA", "Portada", true, true),
            new SeccionPlantillaProvider.SeccionDef("CERTIFICACION_DIRECTOR", "Certificación del Director", true, true),
            new SeccionPlantillaProvider.SeccionDef("AUTORIA", "Autoría", true, false),
            new SeccionPlantillaProvider.SeccionDef("DEDICATORIA", "Dedicatoria", false, false),
            new SeccionPlantillaProvider.SeccionDef("AGRADECIMIENTO", "Agradecimiento", false, false),
            new SeccionPlantillaProvider.SeccionDef("RESUMEN", "Resumen", true, false),
            new SeccionPlantillaProvider.SeccionDef("ABSTRACT", "Abstract", true, false),
            new SeccionPlantillaProvider.SeccionDef("INDICE", "Índice", true, true),
            new SeccionPlantillaProvider.SeccionDef("CODIGO_DUBLIN", "Código Dublín", true, true),
            new SeccionPlantillaProvider.SeccionDef("INTRODUCCION", "Introducción", true, false),
            new SeccionPlantillaProvider.SeccionDef("MARCO_TEORICO", "Marco Teórico", true, false),
            new SeccionPlantillaProvider.SeccionDef("METODOLOGIA", "Metodología", true, false),
            new SeccionPlantillaProvider.SeccionDef("RESULTADOS_DISCUSION", "Resultados y Discusión", true, false),
            new SeccionPlantillaProvider.SeccionDef("CONCLUSIONES_RECOMENDACIONES", "Conclusiones y Recomendaciones", true, false),
            new SeccionPlantillaProvider.SeccionDef("BIBLIOGRAFIA", "Bibliografía", true, false),
            new SeccionPlantillaProvider.SeccionDef("ANEXOS", "Anexos", false, false)
    );

    @BeforeEach
    void setUp() {
        service = new DocumentoTicService(documentoRepo, seccionRepo, versionRepo, comentarioRepo,
                estudianteRepo, tipoTrabajoRepo, docenteRepo, usuarioRepo, authorizationService,
                seccionPlantillaTicService);

        when(seccionPlantillaTicService.plantillaPara(TipoTicPlantilla.PROYECTO_INVESTIGACION))
                .thenReturn(PLANTILLA_PROYECTO_INVESTIGACION);

        when(seccionRepo.save(any())).thenAnswer(inv -> {
            SeccionDocumentoTIC s = inv.getArgument(0);
            almacenadas.removeIf(x -> x.getCodigoSeccion().equals(s.getCodigoSeccion()));
            almacenadas.add(s);
            return s;
        });
        when(seccionRepo.findByDocumento_IdOrderByOrdenAsc(any())).thenAnswer(inv -> new ArrayList<>(almacenadas));
    }

    private Estudiante estudianteConCarrera() {
        Universidad universidad = new Universidad();
        universidad.setNombre("Universidad Técnica Estatal de Quevedo");

        Facultad facultad = new Facultad();
        facultad.setNombre("Facultad de Ciencias de la Computación");
        facultad.setUniversidad(universidad);

        Carrera carrera = new Carrera();
        carrera.setNombre("Ingeniería en Sistemas");
        carrera.setFacultad(facultad);

        Usuario usuario = new Usuario();
        usuario.setNombres("María");
        usuario.setApellidos("González");

        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(10);
        estudiante.setUsuario(usuario);
        estudiante.setCarrera(carrera);
        return estudiante;
    }

    private Docente director() {
        Usuario usuario = new Usuario();
        usuario.setNombres("Carlos");
        usuario.setApellidos("Ramírez");
        Docente docente = new Docente();
        docente.setIdDocente(5);
        docente.setUsuario(usuario);
        docente.setNivelPosgrado(NivelPosgrado.PHD);
        return docente;
    }

    @Test
    @DisplayName("crear() con REDACCION_SISTEMA genera las secciones de la plantilla y auto-llena la PORTADA")
    void crearGeneraSeccionesYAutoLlenaLaPortada() {
        Estudiante estudiante = estudianteConCarrera();
        Docente docente = director();

        Tipotrabajotitulacion tipoTrabajo = new Tipotrabajotitulacion();
        tipoTrabajo.setIdTipoTrabajo(1);
        tipoTrabajo.setNombre("Proyecto de Investigación");

        when(estudianteRepo.findById(10)).thenReturn(Optional.of(estudiante));
        when(tipoTrabajoRepo.findById(1)).thenReturn(Optional.of(tipoTrabajo));
        when(docenteRepo.findById(5)).thenReturn(Optional.of(docente));

        DocumentoTIC[] guardado = new DocumentoTIC[1];
        when(documentoRepo.save(any())).thenAnswer(inv -> {
            guardado[0] = inv.getArgument(0);
            return guardado[0];
        });
        when(documentoRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(guardado[0]));

        CrearDocumentoTicRequest req = new CrearDocumentoTicRequest(
                10, 1, 5, "REDACCION_SISTEMA", "Sistema de gestión de titulación UTEQ");

        DocumentoTicResponse resp = service.crear(req);

        assertThat(resp.secciones()).hasSize(16); // plantilla PROYECTO_INVESTIGACION

        SeccionTicResponse portada = resp.secciones().stream()
                .filter(s -> s.codigoSeccion().equals("PORTADA")).findFirst().orElseThrow();

        assertThat(portada.autoLlenada()).isTrue();
        assertThat(portada.contenidoHtml())
                .contains("Universidad Técnica Estatal de Quevedo")
                .contains("Facultad de Ciencias de la Computación")
                .contains("Ingeniería en Sistemas")
                .contains("Sistema de gestión de titulación UTEQ")
                .contains("María González")
                .contains("PhD. Carlos Ramírez")
                .contains("Quevedo");
    }

    @Test
    @DisplayName("crear() con UPLOAD_PDF no genera ninguna sección")
    void crearConUploadPdfNoGeneraSecciones() {
        Estudiante estudiante = estudianteConCarrera();
        Tipotrabajotitulacion tipoTrabajo = new Tipotrabajotitulacion();
        tipoTrabajo.setIdTipoTrabajo(1);
        tipoTrabajo.setNombre("Proyecto de Investigación");

        when(estudianteRepo.findById(10)).thenReturn(Optional.of(estudiante));
        when(tipoTrabajoRepo.findById(1)).thenReturn(Optional.of(tipoTrabajo));

        DocumentoTIC[] guardado = new DocumentoTIC[1];
        when(documentoRepo.save(any())).thenAnswer(inv -> { guardado[0] = inv.getArgument(0); return guardado[0]; });
        when(documentoRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(guardado[0]));

        CrearDocumentoTicRequest req = new CrearDocumentoTicRequest(
                10, 1, null, "UPLOAD_PDF", "Documento subido");

        DocumentoTicResponse resp = service.crear(req);

        assertThat(resp.secciones()).isEmpty();
    }
}
