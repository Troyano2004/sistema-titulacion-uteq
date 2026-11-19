package com.erwin.backend.service;

import com.erwin.backend.dtos.OportunidadDtos.SolicitarActualizacionRequest;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PensumImportado;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoAsignaturaImportada;
import com.erwin.backend.repository.ActualizacionConocimientosRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.PensumImportadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** FIX 5 de la verificación funcional — Art. 12: plazo de 5 años desde el último periodo aprobado. */
@ExtendWith(MockitoExtension.class)
class ActualizacionConocimientosServiceTest {

    @Mock private ActualizacionConocimientosRepository repo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private PensumImportadoRepository pensumRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private ActualizacionConocimientosService service;

    @BeforeEach
    void setUp() {
        service = new ActualizacionConocimientosService(repo, estudianteRepo, pensumRepo, authorizationService);
    }

    private Estudiante estudiante(Integer id) {
        Estudiante e = new Estudiante();
        ReflectionTestUtils.setField(e, "idEstudiante", id);
        e.setUsuario(new Usuario());
        return e;
    }

    private PensumImportado aprobadaEnPeriodo(String periodo) {
        PensumImportado p = new PensumImportado();
        p.setEstado(EstadoAsignaturaImportada.APROBADA);
        p.setPeriodoAcademico(periodo);
        return p;
    }

    private SolicitarActualizacionRequest req(Integer idEstudiante, String periodoOriginal) {
        return new SolicitarActualizacionRequest(
                idEstudiante, null, List.of("A", "B", "C"), periodoOriginal, null);
    }

    @Test
    @DisplayName("Sin periodo académico aprobado registrado -> 400, no se asume que cumple")
    void sinPeriodoAprobadoRegistradoRechaza() {
        when(estudianteRepo.findById(104)).thenReturn(Optional.of(estudiante(104)));
        when(pensumRepo.findByEstudiante_IdEstudianteAndEstado(104, EstadoAsignaturaImportada.APROBADA))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.solicitar(req(104, "2019-2019")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no tiene un periodo académico aprobado registrado");
    }

    @Test
    @DisplayName("Último periodo aprobado hace más de 5 años -> 400 con el periodo en el mensaje")
    void plazoVencidoRechaza() {
        int anioVencido = LocalDate.now().getYear() - 7;
        when(estudianteRepo.findById(104)).thenReturn(Optional.of(estudiante(104)));
        when(pensumRepo.findByEstudiante_IdEstudianteAndEstado(104, EstadoAsignaturaImportada.APROBADA))
                .thenReturn(List.of(aprobadaEnPeriodo(anioVencido + "-" + anioVencido)));

        assertThatThrownBy(() -> service.solicitar(req(104, "periodo antiguo")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("plazo de 5 años")
                .hasMessageContaining(String.valueOf(anioVencido));
    }

    @Test
    @DisplayName("Último periodo aprobado dentro de los 5 años -> permitido")
    void dentroDelPlazoPermiteSolicitar() {
        int anioReciente = LocalDate.now().getYear() - 2;
        when(estudianteRepo.findById(104)).thenReturn(Optional.of(estudiante(104)));
        when(pensumRepo.findByEstudiante_IdEstudianteAndEstado(104, EstadoAsignaturaImportada.APROBADA))
                .thenReturn(List.of(aprobadaEnPeriodo(anioReciente + "-" + anioReciente)));
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.solicitar(req(104, "periodo reciente"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Toma el periodo APROBADO más reciente cuando hay varios registros")
    void tomaElPeriodoAprobadoMasReciente() {
        int anioViejo = LocalDate.now().getYear() - 8;
        int anioReciente = LocalDate.now().getYear() - 1;
        when(estudianteRepo.findById(104)).thenReturn(Optional.of(estudiante(104)));
        when(pensumRepo.findByEstudiante_IdEstudianteAndEstado(104, EstadoAsignaturaImportada.APROBADA))
                .thenReturn(List.of(
                        aprobadaEnPeriodo(anioViejo + "-" + anioViejo),
                        aprobadaEnPeriodo(anioReciente + "-" + anioReciente)));
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        // Debe usar el más reciente (dentro del plazo) y no rechazar por el más viejo.
        assertThatCode(() -> service.solicitar(req(104, "x"))).doesNotThrowAnyException();
    }
}
