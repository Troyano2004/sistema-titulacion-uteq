package com.erwin.backend.service;

import com.erwin.backend.dtos.AprobarYDesignarRequest;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.NivelPosgrado;
import com.erwin.backend.enums.TipoVinculacion;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Elegibilidad de Director/Tribunal del TIC al resolver la propuesta en
 * Consejo Directivo (PropuestaResolucionService.aprobarYDesignar): docente de
 * planta UTEQ, con posgrado, vinculado a la carrera de la propuesta.
 */
@ExtendWith(MockitoExtension.class)
class PropuestaResolucionServiceTest {

    @Mock private PropuestaTitulacionRepository propuestaRepo;
    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private DocenteCarreraRepository docenteCarreraRepo;
    @Mock private TribunalProyectoRepository tribunalRepo;
    @Mock private UsuarioRepository usuarioRepo;

    private PropuestaResolucionService service;

    @BeforeEach
    void setUp() {
        service = new PropuestaResolucionService(
                propuestaRepo, proyectoRepo, docenteRepo, docenteCarreraRepo, tribunalRepo, usuarioRepo);
    }

    private Carrera carrera() {
        Carrera c = new Carrera();
        c.setIdCarrera(7);
        c.setNombre("Software");
        return c;
    }

    private PropuestaTitulacion propuestaAprobada() {
        PropuestaTitulacion p = new PropuestaTitulacion();
        p.setIdPropuesta(1);
        p.setEstado("APROBADA");
        p.setCarrera(carrera());
        p.setResolucionConsejoDirectivo(false);
        return p;
    }

    private ProyectoTitulacion proyecto() {
        ProyectoTitulacion pr = new ProyectoTitulacion();
        pr.setIdProyecto(50);
        return pr;
    }

    private Docente docente(int id, TipoVinculacion vinculacion, NivelPosgrado posgrado) {
        Docente d = new Docente();
        d.setIdDocente(id);
        d.setTipoVinculacion(vinculacion);
        d.setNivelPosgrado(posgrado);
        Usuario u = new Usuario();
        u.setNombres("Doc" + id);
        u.setApellidos("Apellido" + id);
        d.setUsuario(u);
        return d;
    }

    private AprobarYDesignarRequest request(int director, int presidente, int vocal1, int vocal2) {
        AprobarYDesignarRequest req = new AprobarYDesignarRequest();
        req.setIdUsuario(999);
        req.setDirectorId(director);
        req.setPresidenteTribunalId(presidente);
        req.setVocal1Id(vocal1);
        req.setVocal2Id(vocal2);
        return req;
    }

    private void stubComunes() {
        when(propuestaRepo.findById(1)).thenReturn(Optional.of(propuestaAprobada()));
        when(proyectoRepo.findByPropuesta_IdPropuesta(1)).thenReturn(Optional.of(proyecto()));
    }

    @Test
    @DisplayName("Rechaza como Director a un docente que no es de planta")
    void rechazaDirectorNoPlanta() {
        stubComunes();
        when(docenteRepo.findById(10)).thenReturn(Optional.of(
                docente(10, TipoVinculacion.CONTRATADO, NivelPosgrado.MAGISTER)));

        assertThatThrownBy(() -> service.aprobarYDesignar(1, request(10, 11, 12, 13)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("planta");
    }

    @Test
    @DisplayName("Rechaza como Director a un docente de planta sin posgrado")
    void rechazaDirectorSinPosgrado() {
        stubComunes();
        when(docenteRepo.findById(10)).thenReturn(Optional.of(
                docente(10, TipoVinculacion.PLANTA, NivelPosgrado.NINGUNO)));

        assertThatThrownBy(() -> service.aprobarYDesignar(1, request(10, 11, 12, 13)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posgrado");
    }

    @Test
    @DisplayName("Rechaza como Director a un docente no vinculado a la carrera de la propuesta")
    void rechazaDirectorNoVinculadoACarrera() {
        stubComunes();
        when(docenteRepo.findById(10)).thenReturn(Optional.of(
                docente(10, TipoVinculacion.PLANTA, NivelPosgrado.MAGISTER)));
        when(docenteCarreraRepo.existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(10, 7)).thenReturn(false);

        assertThatThrownBy(() -> service.aprobarYDesignar(1, request(10, 11, 12, 13)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("área afín");
    }

    @Test
    @DisplayName("Rechaza si el Director y algún miembro del Tribunal se repiten")
    void rechazaIdsDuplicados() {
        when(propuestaRepo.findById(1)).thenReturn(Optional.of(propuestaAprobada()));

        assertThatThrownBy(() -> service.aprobarYDesignar(1, request(10, 10, 12, 13)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distintos");

        verifyNoInteractions(docenteRepo);
    }

    @Test
    @DisplayName("Rechaza resolver una propuesta que no está APROBADA por la Comisión")
    void rechazaPropuestaNoAprobada() {
        PropuestaTitulacion p = propuestaAprobada();
        p.setEstado("EN_REVISION");
        when(propuestaRepo.findById(1)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.aprobarYDesignar(1, request(10, 11, 12, 13)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Designa Director y Tribunal cuando los 4 docentes son elegibles")
    void designaCorrectamenteConDocentesElegibles() {
        stubComunes();
        when(docenteRepo.findById(10)).thenReturn(Optional.of(docente(10, TipoVinculacion.PLANTA, NivelPosgrado.PHD)));
        when(docenteRepo.findById(11)).thenReturn(Optional.of(docente(11, TipoVinculacion.PLANTA, NivelPosgrado.MAGISTER)));
        when(docenteRepo.findById(12)).thenReturn(Optional.of(docente(12, TipoVinculacion.PLANTA, NivelPosgrado.MAGISTER)));
        when(docenteRepo.findById(13)).thenReturn(Optional.of(docente(13, TipoVinculacion.PLANTA, NivelPosgrado.ESPECIALISTA)));
        when(docenteCarreraRepo.existsByDocente_IdDocenteAndCarrera_IdCarreraAndActivoTrue(any(), eq(7))).thenReturn(true);
        when(usuarioRepo.findById(999)).thenReturn(Optional.of(new Usuario()));
        when(proyectoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propuestaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.aprobarYDesignar(1, request(10, 11, 12, 13));

        assertThat(response.getDirector()).contains("Doc10");
        assertThat(response.getPresidenteTribunal()).contains("Doc11");
        verify(tribunalRepo).deleteByProyecto_IdProyecto(50);
        verify(tribunalRepo, times(3)).save(any(TribunalProyecto.class));
        verify(proyectoRepo).save(any(ProyectoTitulacion.class));
    }
}
