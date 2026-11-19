package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.ConfiguracionModalidadEntregaRequest;
import com.erwin.backend.dtos.ConfiguracionModalidadEntregaResponse;
import com.erwin.backend.entities.Dt1Asignacion;
import com.erwin.backend.enums.ModalidadEntrega;
import com.erwin.backend.repository.Dt1AsignacionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfiguracionTitulacionService {

    private static final ModalidadEntrega MODALIDAD_ENTREGA_DEFECTO = ModalidadEntrega.REDACCION_SISTEMA;

    private final Dt1AsignacionRepository dt1AsignacionRepo;

    public ConfiguracionTitulacionService(Dt1AsignacionRepository dt1AsignacionRepo) {
        this.dt1AsignacionRepo = dt1AsignacionRepo;
    }

    @Auditable(entidad = "Dt1Asignacion", accion = "CONFIGURAR_MODALIDAD_ENTREGA")
    public ConfiguracionModalidadEntregaResponse configurar(ConfiguracionModalidadEntregaRequest req) {
        if (req == null || req.getIdDocente() == null || req.getIdCarrera() == null || req.getIdPeriodo() == null) {
            throw new IllegalArgumentException("idDocente, idCarrera e idPeriodo son obligatorios");
        }
        ModalidadEntrega modalidad = parseModalidad(req.getModalidadEntrega());

        Dt1Asignacion asignacion = dt1AsignacionRepo
                .findByDocente_IdDocenteAndCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrue(
                        req.getIdDocente(), req.getIdCarrera(), req.getIdPeriodo())
                .orElseThrow(() -> new EntityNotFoundException(
                        "El docente no tiene una asignación DT1 activa para esa carrera y periodo"));

        asignacion.setModalidadEntrega(modalidad);
        dt1AsignacionRepo.save(asignacion);

        return new ConfiguracionModalidadEntregaResponse(req.getIdCarrera(), req.getIdPeriodo(), modalidad.name());
    }

    @Transactional(readOnly = true)
    public ConfiguracionModalidadEntregaResponse obtener(Integer idCarrera, Integer idPeriodo) {
        ModalidadEntrega modalidad = dt1AsignacionRepo
                .findFirstByCarrera_IdCarreraAndPeriodo_IdPeriodoAndActivoTrueAndModalidadEntregaIsNotNull(idCarrera, idPeriodo)
                .map(Dt1Asignacion::getModalidadEntrega)
                .orElse(MODALIDAD_ENTREGA_DEFECTO);

        return new ConfiguracionModalidadEntregaResponse(idCarrera, idPeriodo, modalidad.name());
    }

    private ModalidadEntrega parseModalidad(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("modalidadEntrega es obligatoria");
        }
        try {
            return ModalidadEntrega.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("modalidadEntrega inválida: " + valor);
        }
    }
}
