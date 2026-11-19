package com.erwin.backend.service;

import com.erwin.backend.dtos.ExpedienteDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoDocumentoExpediente;
import com.erwin.backend.enums.ModalidadCatalogoDocumento;
import com.erwin.backend.enums.ResponsableDocumento;
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

/** TAREA 1: validación de expediente COMPLETO vs INCOMPLETO. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpedienteServiceTest {

    @Mock private ExpedienteEstudianteRepository expedienteRepo;
    @Mock private DocumentoExpedienteRepository documentoRepo;
    @Mock private CatalogoDocumentoHabilitanteRepository catalogoRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private SustentacionRepository sustentacionRepo;
    @Mock private ExamenComplexivoAplicacionRepository examenRepo;
    @Mock private PeriodoTitulacionRepository periodoRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private ExpedienteService service;
    private final List<ExpedienteEstudiante> expedientesGuardados = new ArrayList<>();
    private final List<DocumentoExpediente> documentosGuardados = new ArrayList<>();
    private long siguienteIdExpediente = 1;
    private long siguienteIdDocumento = 1;

    @BeforeEach
    void setUp() {
        service = new ExpedienteService(expedienteRepo, documentoRepo, catalogoRepo, estudianteRepo,
                usuarioRepo, proyectoRepo, sustentacionRepo, examenRepo, periodoRepo, authorizationService);

        when(expedienteRepo.save(any())).thenAnswer(inv -> {
            ExpedienteEstudiante e = inv.getArgument(0);
            if (e.getId() == null) e.setId(siguienteIdExpediente++);
            expedientesGuardados.removeIf(x -> x.getId().equals(e.getId()));
            expedientesGuardados.add(e);
            return e;
        });
        when(expedienteRepo.findById(any())).thenAnswer(inv ->
                expedientesGuardados.stream().filter(e -> e.getId().equals(inv.getArgument(0))).findFirst());

        when(documentoRepo.save(any())).thenAnswer(inv -> {
            DocumentoExpediente d = inv.getArgument(0);
            if (d.getId() == null) d.setId(siguienteIdDocumento++);
            documentosGuardados.removeIf(x -> x.getId().equals(d.getId()));
            documentosGuardados.add(d);
            return d;
        });
        when(documentoRepo.findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(any())).thenAnswer(inv ->
                documentosGuardados.stream().filter(d -> d.getExpediente().getId().equals(inv.getArgument(0))).toList());
        when(documentoRepo.findById(any())).thenAnswer(inv ->
                documentosGuardados.stream().filter(d -> d.getId().equals(inv.getArgument(0))).findFirst());
    }

    private Estudiante estudiante() {
        Usuario u = new Usuario();
        u.setNombres("Luis");
        u.setApellidos("Mora");
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(1);
        Estudiante e = new Estudiante();
        e.setIdEstudiante(20);
        e.setUsuario(u);
        e.setCarrera(carrera);
        return e;
    }

    private CatalogoDocumentoHabilitante catalogo(String codigo, boolean obligatorio) {
        CatalogoDocumentoHabilitante c = new CatalogoDocumentoHabilitante();
        c.setId((long) codigo.hashCode());
        c.setCodigo(codigo);
        c.setNombre("Documento " + codigo);
        c.setModalidad(ModalidadCatalogoDocumento.TIC);
        c.setOrden((int) codigo.charAt(0));
        c.setObligatorio(obligatorio);
        c.setResponsable(ResponsableDocumento.ESTUDIANTE);
        return c;
    }

    @Test
    @DisplayName("crear() genera un DocumentoExpediente por cada ítem del catálogo y el expediente queda INCOMPLETO")
    void crearGeneraDocumentosYQuedaIncompleto() {
        when(estudianteRepo.findById(20)).thenReturn(Optional.of(estudiante()));
        when(catalogoRepo.findByModalidadInOrderByOrdenAsc(any()))
                .thenReturn(List.of(catalogo("A", true), catalogo("B", true)));

        ExpedienteDto dto = service.crear(new CrearExpedienteRequest(20, "TIC", "2025-2025"));

        assertThat(dto.documentos()).hasSize(2);
        assertThat(dto.estado()).isEqualTo("INCOMPLETO");
    }

    @Test
    @DisplayName("el expediente pasa a COMPLETO cuando todos los documentos obligatorios quedan VALIDADO")
    void expedienteQuedaCompletoConTodoValidado() {
        when(estudianteRepo.findById(20)).thenReturn(Optional.of(estudiante()));
        when(catalogoRepo.findByModalidadInOrderByOrdenAsc(any()))
                .thenReturn(List.of(catalogo("A", true), catalogo("B", true)));
        when(usuarioRepo.findById(99)).thenReturn(Optional.of(new Usuario()));

        ExpedienteDto creado = service.crear(new CrearExpedienteRequest(20, "TIC", "2025-2025"));
        List<DocumentoExpedienteDto> docs = creado.documentos();

        for (DocumentoExpedienteDto d : docs) {
            service.cargar(d.id(), new CargarDocumentoRequest("http://blob/archivo.pdf", null));
            service.validar(d.id(), new ValidarDocumentoRequest(99));
        }

        ExpedienteDto actualizado = service.obtenerPorId(creado.id());
        assertThat(actualizado.estado()).isEqualTo("COMPLETO");
    }

    @Test
    @DisplayName("el expediente permanece INCOMPLETO si falta validar un documento obligatorio")
    void expedientePermaneceIncompletoSiFaltaUnDocumento() {
        when(estudianteRepo.findById(20)).thenReturn(Optional.of(estudiante()));
        when(catalogoRepo.findByModalidadInOrderByOrdenAsc(any()))
                .thenReturn(List.of(catalogo("A", true), catalogo("B", true)));
        when(usuarioRepo.findById(99)).thenReturn(Optional.of(new Usuario()));

        ExpedienteDto creado = service.crear(new CrearExpedienteRequest(20, "TIC", "2025-2025"));
        DocumentoExpedienteDto primero = creado.documentos().get(0);

        service.cargar(primero.id(), new CargarDocumentoRequest("http://blob/archivo.pdf", null));
        service.validar(primero.id(), new ValidarDocumentoRequest(99));
        // El segundo documento queda PENDIENTE

        ExpedienteDto actualizado = service.obtenerPorId(creado.id());
        assertThat(actualizado.estado()).isEqualTo("INCOMPLETO");
    }

    @Test
    @DisplayName("rechazar() exige un motivo y deja el expediente INCOMPLETO")
    void rechazarExigeMotivo() {
        when(estudianteRepo.findById(20)).thenReturn(Optional.of(estudiante()));
        when(catalogoRepo.findByModalidadInOrderByOrdenAsc(any())).thenReturn(List.of(catalogo("A", true)));
        when(usuarioRepo.findById(99)).thenReturn(Optional.of(new Usuario()));

        ExpedienteDto creado = service.crear(new CrearExpedienteRequest(20, "TIC", "2025-2025"));
        DocumentoExpedienteDto doc = creado.documentos().get(0);
        service.cargar(doc.id(), new CargarDocumentoRequest("http://blob/archivo.pdf", null));

        DocumentoExpedienteDto rechazado = service.rechazar(doc.id(), new RechazarDocumentoRequest(99, "Documento ilegible"));

        assertThat(rechazado.estado()).isEqualTo("RECHAZADO");
        assertThat(service.obtenerPorId(creado.id()).estado()).isEqualTo("INCOMPLETO");
    }
}
