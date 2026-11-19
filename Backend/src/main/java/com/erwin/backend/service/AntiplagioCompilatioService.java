package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.AntiplagioResultadoResponse;
import com.erwin.backend.dtos.CertificadoCompilatioResponse;
import com.erwin.backend.dtos.RegistrarResultadoAntiplagioRequest;
import com.erwin.backend.entities.AntiplacioIntento;
import com.erwin.backend.entities.DocumentoTitulacion;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.enums.EstadoDocumento;
import com.erwin.backend.repository.AntiplacioIntentoRepository;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Módulo 8: registro manual del resultado COMPILATIO (el Director escribe el
 * porcentaje) y emisión del certificado antiplagio. Entrada alternativa a
 * Dt2Service.registrarAntiplagio (que recibe idProyecto + archivo en
 * multipart): esta acepta estudianteId + un archivoReporteId ya subido.
 */
@Service
@Transactional
public class AntiplagioCompilatioService {

    private static final BigDecimal UMBRAL_ANTIPLAGIO = new BigDecimal("10.00");

    private final ProyectoTitulacionRepository proyectoRepo;
    private final DocumentoTitulacionRepository documentoRepo;
    private final AntiplacioIntentoRepository antiplagioRepo;

    public AntiplagioCompilatioService(ProyectoTitulacionRepository proyectoRepo,
                                        DocumentoTitulacionRepository documentoRepo,
                                        AntiplacioIntentoRepository antiplagioRepo) {
        this.proyectoRepo = proyectoRepo;
        this.documentoRepo = documentoRepo;
        this.antiplagioRepo = antiplagioRepo;
    }

    @Auditable(entidad = "AntiplacioIntento", accion = "REGISTRAR_RESULTADO_COMPILATIO")
    public AntiplagioResultadoResponse registrarResultado(RegistrarResultadoAntiplagioRequest req) {
        if (req == null || req.getEstudianteId() == null) {
            throw new IllegalArgumentException("estudianteId es obligatorio");
        }
        if (req.getPorcentaje() == null
                || req.getPorcentaje().compareTo(BigDecimal.ZERO) < 0
                || req.getPorcentaje().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }

        ProyectoTitulacion proyecto = getProyectoDeEstudiante(req.getEstudianteId());
        if (proyecto.getDirector() == null) {
            throw new IllegalStateException("El proyecto no tiene Director asignado; no se puede registrar el resultado");
        }

        boolean favorable = req.getPorcentaje().compareTo(UMBRAL_ANTIPLAGIO) < 0;

        AntiplacioIntento intento = new AntiplacioIntento();
        intento.setProyecto(proyecto);
        intento.setDirector(proyecto.getDirector());
        intento.setPorcentajeCoincidencia(req.getPorcentaje());
        intento.setArchivoReporteId(req.getArchivoReporteId());
        intento.setFavorable(favorable);
        intento.setObservaciones(req.getObservaciones());
        antiplagioRepo.save(intento);

        proyecto.setPorcentajeAntiplagio(req.getPorcentaje());
        proyecto.setFechaVerificacionAntiplagio(LocalDate.now());
        proyectoRepo.save(proyecto);

        String mensaje;
        if (favorable) {
            mensaje = "Resultado registrado. El documento puede avanzar a predefensa.";
            documentoRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto()).ifPresent(doc -> {
                if (doc.getEstado() == EstadoDocumento.APROBADO_POR_DIRECTOR) {
                    doc.setEstado(EstadoDocumento.ANTIPLAGIO_APROBADO);
                    documentoRepo.save(doc);
                    proyecto.setEstado("PREDEFENSA");
                    proyectoRepo.save(proyecto);
                }
            });
        } else {
            mensaje = "El porcentaje de coincidencia (" + req.getPorcentaje()
                    + "%) supera el máximo permitido (10%). El estudiante debe revisar y corregir el documento";
        }

        return new AntiplagioResultadoResponse(
                intento.getIdIntento(), proyecto.getIdProyecto(), intento.getPorcentajeCoincidencia(),
                favorable, mensaje, intento.getFechaIntento());
    }

    @Auditable(entidad = "AntiplacioIntento", accion = "EMITIR_CERTIFICADO")
    public CertificadoCompilatioResponse emitirCertificado(Integer estudianteId) {
        ProyectoTitulacion proyecto = getProyectoDeEstudiante(estudianteId);

        List<AntiplacioIntento> historial = antiplagioRepo.findByProyecto_IdProyectoOrderByFechaIntentoDesc(proyecto.getIdProyecto());
        AntiplacioIntento ultimo = historial.isEmpty() ? null : historial.get(0);
        if (ultimo == null) {
            throw new IllegalStateException("No hay ningún resultado de COMPILATIO registrado para este estudiante");
        }
        if (ultimo.getPorcentajeCoincidencia().compareTo(UMBRAL_ANTIPLAGIO) >= 0) {
            throw new IllegalStateException(
                    "El porcentaje de coincidencia (" + ultimo.getPorcentajeCoincidencia()
                            + "%) supera el máximo permitido (10%). El estudiante debe revisar y corregir el documento");
        }

        ultimo.setCertificadoEmitido(true);
        ultimo.setFechaEmisionCertificado(LocalDateTime.now());
        antiplagioRepo.save(ultimo);

        String firmadoPor = proyecto.getDirector() != null && proyecto.getDirector().getUsuario() != null
                ? (proyecto.getDirector().getUsuario().getNombres() + " " + proyecto.getDirector().getUsuario().getApellidos()).trim()
                : null;

        return new CertificadoCompilatioResponse(
                ultimo.getIdIntento(), proyecto.getIdProyecto(), ultimo.getPorcentajeCoincidencia(),
                true, ultimo.getFechaEmisionCertificado(), firmadoPor);
    }

    private ProyectoTitulacion getProyectoDeEstudiante(Integer estudianteId) {
        return proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El estudiante no tiene un proyecto de titulación registrado: " + estudianteId));
    }
}
