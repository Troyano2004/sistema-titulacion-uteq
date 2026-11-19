package com.erwin.backend.service;

import com.erwin.backend.dtos.AnteproyectoVersionRequest;
import com.erwin.backend.entities.AnteproyectoTitulacion;
import com.erwin.backend.enums.EstadoAnteproyecto;
import com.erwin.backend.repository.AnteproyectoTitulacionRepository;
import com.erwin.backend.repository.AnteproyectoVersionRepository;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.repository.PropuestaRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import com.erwin.backend.repository.TribunalProyectoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el bloqueo de edición del anteproyecto una vez que ya salió de
 * BORRADOR (regla verificada en AnteproyectoService.estaBloqueado()).
 */
@ExtendWith(MockitoExtension.class)
class AnteproyectoServiceTest {

    @Mock private AnteproyectoVersionRepository verRepo;
    @Mock private PropuestaRepository propRepo;
    @Mock private AnteproyectoTitulacionRepository anteRepo;
    @Mock private ProyectoTitulacionRepository proyectoRepo;
    @Mock private TribunalProyectoRepository tribunalRepo;
    @Mock private DocenteRepository docenteRepo;
    @Mock private com.erwin.backend.security.AuthorizationService authorizationService;

    private AnteproyectoService anteproyectoService;

    @BeforeEach
    void setUp() {
        anteproyectoService = new AnteproyectoService(verRepo, propRepo, anteRepo, proyectoRepo, tribunalRepo, docenteRepo, authorizationService);
    }

    private AnteproyectoTitulacion anteproyectoConEstado(EstadoAnteproyecto estado) {
        AnteproyectoTitulacion ante = new AnteproyectoTitulacion();
        ante.setIdAnteproyecto(1);
        ante.setEstado(estado);
        return ante;
    }

    @ParameterizedTest(name = "estado {0} bloquea la edición")
    @EnumSource(value = EstadoAnteproyecto.class, names = {"APROBADO", "RECHAZADO", "EN_REVISION"})
    @DisplayName("No se puede editar (guardar borrador) un anteproyecto APROBADO, RECHAZADO o EN_REVISION")
    void noSePuedeEditarUnAnteproyectoBloqueado(EstadoAnteproyecto estadoBloqueante) {
        when(anteRepo.findById(1)).thenReturn(Optional.of(anteproyectoConEstado(estadoBloqueante)));

        AnteproyectoVersionRequest req = new AnteproyectoVersionRequest();
        req.titulo = "Nuevo título";

        assertThatThrownBy(() -> anteproyectoService.guardarBorrador(1, req))
                .hasMessageContaining("ANTEPROYECTO_BLOQUEADO");

        verify(verRepo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Sí se puede editar (guardar borrador) un anteproyecto en BORRADOR")
    void siSePuedeEditarUnAnteproyectoEnBorrador() {
        AnteproyectoTitulacion ante = anteproyectoConEstado(EstadoAnteproyecto.BORRADOR);
        when(anteRepo.findById(1)).thenReturn(Optional.of(ante));
        when(verRepo.maxNumeroVersion(1)).thenReturn(0);
        when(verRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        AnteproyectoVersionRequest req = new AnteproyectoVersionRequest();
        req.titulo = "Nuevo título";

        anteproyectoService.guardarBorrador(1, req);

        verify(anteRepo).save(ante);
        org.junit.jupiter.api.Assertions.assertEquals(EstadoAnteproyecto.BORRADOR, ante.getEstado());
    }
}
