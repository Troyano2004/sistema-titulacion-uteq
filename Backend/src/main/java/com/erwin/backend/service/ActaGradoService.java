package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.IncorporacionDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoActaGrado;
import com.erwin.backend.enums.EstadoDeudaImportada;
import com.erwin.backend.enums.EstadoExpediente;
import com.erwin.backend.enums.ModalidadActaGrado;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Módulo 19 (TAREA 1) — incorporación y registro. NO recalcula la nota de
 * grado desde cero: reutiliza el acta que Dt2Service/ComplexivoService ya
 * generaron (notaRecord/notaTitulacion/notaFinal con la fórmula 80/20) y
 * agrega la capa de firmas/ceremonia/SENESCYT encima.
 */
@Service
@Transactional
public class ActaGradoService {

    private static final BigDecimal PESO_RECORD = new BigDecimal("0.80");
    private static final BigDecimal PESO_TITULACION = new BigDecimal("0.20");

    private final ActaGradoRepository actaGradoRepo;
    private final EstudianteRepository estudianteRepo;
    private final ExpedienteEstudianteRepository expedienteRepo;
    private final DeudaImportadaRepository deudaRepo;
    private final AuthorizationService authorizationService;

    public ActaGradoService(ActaGradoRepository actaGradoRepo,
                             EstudianteRepository estudianteRepo,
                             ExpedienteEstudianteRepository expedienteRepo,
                             DeudaImportadaRepository deudaRepo,
                             AuthorizationService authorizationService) {
        this.actaGradoRepo = actaGradoRepo;
        this.estudianteRepo = estudianteRepo;
        this.expedienteRepo = expedienteRepo;
        this.deudaRepo = deudaRepo;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Generar (TAREA 1.3) — valida expediente COMPLETO y sin deudas pendientes
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ActaGrado", accion = "GENERAR_INCORPORACION")
    public ActaGradoIncorporacionResponse generar(Integer idEstudiante) {
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));

        ExpedienteEstudiante expediente = expedienteRepo
                .findFirstByEstudiante_IdEstudianteOrderByIdDesc(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El estudiante no tiene un expediente digital creado"));
        if (expediente.getEstado() != EstadoExpediente.COMPLETO
                && expediente.getEstado() != EstadoExpediente.VALIDADO
                && expediente.getEstado() != EstadoExpediente.LEGALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El expediente del estudiante debe estar COMPLETO antes de generar el acta de grado "
                            + "(estado actual: " + expediente.getEstado() + ")");
        }

        if (deudaRepo.existsByEstudiante_IdEstudianteAndEstado(idEstudiante, EstadoDeudaImportada.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante registra deudas administrativas pendientes importadas del SGA");
        }

        ActaGrado acta = actaGradoRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El estudiante todavía no tiene una nota de grado calculada "
                                + "(debe completarse la sustentación o el examen complexivo primero)"));

        if (acta.getNotaRecord() == null || acta.getNotaTitulacion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El acta no tiene registrados el promedio del récord académico y la nota de titulación");
        }

        // Misma fórmula 80/20 ya usada por Dt2Service/ComplexivoService — se recalcula aquí
        // para que la capa de incorporación no dependa ciegamente de un valor ya guardado.
        BigDecimal notaFinalGrado = acta.getNotaRecord().multiply(PESO_RECORD)
                .add(acta.getNotaTitulacion().multiply(PESO_TITULACION))
                .setScale(2, RoundingMode.HALF_UP);

        acta.setPromedioRecordAcademico(acta.getNotaRecord());
        acta.setNotaFinalGrado(notaFinalGrado);
        acta.setModalidad("EXAMEN_COMPLEXIVO".equals(acta.getTipoTitulacion())
                ? ModalidadActaGrado.EXAMEN_COMPLEXIVO : ModalidadActaGrado.TIC);
        if (acta.getEstado() == null) {
            acta.setEstado(EstadoActaGrado.GENERADA);
        }

        return toDto(actaGradoRepo.save(acta));
    }

    // ═══════════════════════════════════════════════════════════════
    // Firmar (TAREA 1.3)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ActaGrado", accion = "FIRMAR")
    public ActaGradoIncorporacionResponse firmar(Integer idActa, FirmarActaRequest req) {
        ActaGrado acta = getActa(idActa);
        if (acta.getEstado() == EstadoActaGrado.REGISTRADA_SENESCYT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El acta ya está registrada en SENESCYT");
        }

        String tipo = req.tipoFirma() == null ? "" : req.tipoFirma().trim().toUpperCase();
        switch (tipo) {
            case "RECTOR" -> acta.setFirmaRector(true);
            case "DECANO" -> acta.setFirmaDecano(true);
            case "SECRETARIO_GENERAL" -> acta.setFirmaSecretarioGeneral(true);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de firma inválido: " + req.tipoFirma() + ". Use RECTOR, DECANO o SECRETARIO_GENERAL");
        }

        if (Boolean.TRUE.equals(acta.getFirmaRector())
                && Boolean.TRUE.equals(acta.getFirmaDecano())
                && Boolean.TRUE.equals(acta.getFirmaSecretarioGeneral())) {
            acta.setEstado(EstadoActaGrado.FIRMADA);
        }

        return toDto(actaGradoRepo.save(acta));
    }

    @Auditable(entidad = "ActaGrado", accion = "PROGRAMAR_CEREMONIA")
    public ActaGradoIncorporacionResponse programarCeremonia(Integer idActa, ProgramarCeremoniaRequest req) {
        ActaGrado acta = getActa(idActa);
        acta.setFechaCeremonia(req.fechaCeremonia());
        return toDto(actaGradoRepo.save(acta));
    }

    /** Marcado por TituloProfesionalService al registrar el número de SENESCYT. */
    void marcarRegistradaSenescyt(ActaGrado acta) {
        acta.setEstado(EstadoActaGrado.REGISTRADA_SENESCYT);
        actaGradoRepo.save(acta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Pendientes de incorporación (TAREA 1.3)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<PendienteIncorporacionDto> pendientes(Integer idFacultad) {
        return actaGradoRepo.findAll().stream()
                .filter(a -> a.getEstado() == EstadoActaGrado.GENERADA || a.getEstado() == EstadoActaGrado.FIRMADA)
                .filter(a -> idFacultad == null || coincideFacultad(a, idFacultad))
                .map(this::toPendienteDto)
                .collect(Collectors.toList());
    }

    private boolean coincideFacultad(ActaGrado acta, Integer idFacultad) {
        Carrera carrera = acta.getEstudiante() != null ? acta.getEstudiante().getCarrera() : null;
        return carrera != null && carrera.getFacultad() != null && idFacultad.equals(carrera.getFacultad().getIdFacultad());
    }

    @Transactional(readOnly = true)
    public ActaGradoIncorporacionResponse obtenerPorEstudiante(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        ActaGrado acta = actaGradoRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTA_NO_ENCONTRADA"));
        return toDto(acta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    ActaGrado getActa(Integer idActa) {
        return actaGradoRepo.findById(idActa)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTA_NO_ENCONTRADA"));
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private ActaGradoIncorporacionResponse toDto(ActaGrado a) {
        Estudiante est = a.getEstudiante();
        return new ActaGradoIncorporacionResponse(
                a.getIdActa(),
                est != null ? est.getIdEstudiante() : null,
                est != null ? nombreCompleto(est.getUsuario()) : null,
                a.getTipoTitulacion(),
                a.getModalidad() != null ? a.getModalidad().name() : null,
                a.getPromedioRecordAcademico(),
                a.getNotaTitulacion(),
                a.getNotaFinalGrado(),
                a.getEstado() != null ? a.getEstado().name() : null,
                a.getFirmaRector(),
                a.getFirmaDecano(),
                a.getFirmaSecretarioGeneral(),
                a.getFechaCeremonia(),
                a.getFechaEmision()
        );
    }

    private PendienteIncorporacionDto toPendienteDto(ActaGrado a) {
        Estudiante est = a.getEstudiante();
        Carrera carrera = est != null ? est.getCarrera() : null;
        Facultad facultad = carrera != null ? carrera.getFacultad() : null;
        return new PendienteIncorporacionDto(
                a.getIdActa(),
                est != null ? est.getIdEstudiante() : null,
                est != null ? nombreCompleto(est.getUsuario()) : null,
                carrera != null ? carrera.getIdCarrera() : null,
                carrera != null ? carrera.getNombre() : null,
                facultad != null ? facultad.getIdFacultad() : null,
                a.getEstado() != null ? a.getEstado().name() : null,
                a.getFirmaRector(),
                a.getFirmaDecano(),
                a.getFirmaSecretarioGeneral()
        );
    }
}
