package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.OportunidadDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.*;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Módulo 13 — control de oportunidades y reprobación. Se apoya en
 * {@link RegistroOportunidad} (una fila por cada intento FINAL o SUPLETORIO,
 * en cualquiera de las dos modalidades de titulación) para aplicar los
 * límites del Reglamento: máximo 2 intentos por periodo, SUPLETORIO solo en
 * la primera ocasión, máximo 2 periodos de titulación sin autorización
 * especial, y el requisito de actualización de conocimientos tras una
 * reprobación en 2da/3ra ocasión o tras 5 años sin actividad.
 */
@Service
@Transactional
public class OportunidadService {

    private static final int MAX_OPORTUNIDADES_POR_PERIODO = 2;
    private static final int MAX_PERIODOS_SIN_AUTORIZACION = 2;
    private static final int ANIOS_PLAZO_REINGRESO = 5;

    private final RegistroOportunidadRepository registroRepo;
    private final ActualizacionConocimientosRepository actualizacionRepo;
    private final AutorizacionTerceraMatriculaRepository autorizacionRepo;
    private final EstudianteRepository estudianteRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuthorizationService authorizationService;

    public OportunidadService(RegistroOportunidadRepository registroRepo,
                               ActualizacionConocimientosRepository actualizacionRepo,
                               AutorizacionTerceraMatriculaRepository autorizacionRepo,
                               EstudianteRepository estudianteRepo,
                               PeriodoTitulacionRepository periodoRepo,
                               UsuarioRepository usuarioRepo,
                               AuthorizationService authorizationService) {
        this.registroRepo = registroRepo;
        this.actualizacionRepo = actualizacionRepo;
        this.autorizacionRepo = autorizacionRepo;
        this.estudianteRepo = estudianteRepo;
        this.periodoRepo = periodoRepo;
        this.usuarioRepo = usuarioRepo;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Registrar resultado — invocado por Dt2Service / ExamenComplexivoService
    // al consolidar un resultado de examen o sustentación (TAREA integración).
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "RegistroOportunidad", accion = "REGISTRAR")
    public RegistroOportunidad registrarResultado(Estudiante estudiante, PeriodoTitulacion periodo,
                                                   ModalidadOportunidad modalidad, TipoOportunidad tipoOportunidad,
                                                   ResultadoOportunidad resultado, BigDecimal calificacion,
                                                   LocalDate fecha, String observaciones) {
        RegistroOportunidad registro = new RegistroOportunidad();
        registro.setEstudiante(estudiante);
        registro.setPeriodoTitulacion(periodo);
        registro.setModalidad(modalidad);
        registro.setTipoOportunidad(tipoOportunidad);
        registro.setResultado(resultado);
        registro.setCalificacion(calificacion);
        registro.setFecha(fecha != null ? fecha : LocalDate.now());
        registro.setObservaciones(observaciones);
        return registroRepo.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroOportunidadDto> historial(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return registroRepo.findByEstudiante_IdEstudianteOrderByFechaAsc(idEstudiante)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // Elegibilidad (TAREA 1.3.a-e)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ElegibilidadDto puedeRendir(Integer idEstudiante, TipoOportunidad tipoOportunidad) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        Estudiante estudiante = estudianteRepo.findById(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));
        PeriodoTitulacion periodoActivo = getPeriodoActivo();

        List<RegistroOportunidad> historialCompleto = registroRepo
                .findByEstudiante_IdEstudianteOrderByFechaAsc(idEstudiante);

        // a) Máximo 2 oportunidades (FINAL + SUPLETORIO) en el periodo actual
        long enPeriodoActual = historialCompleto.stream()
                .filter(r -> r.getPeriodoTitulacion().getIdPeriodo().equals(periodoActivo.getIdPeriodo()))
                .count();
        if (enPeriodoActual >= MAX_OPORTUNIDADES_POR_PERIODO) {
            return new ElegibilidadDto(false,
                    "Ya se registraron " + enPeriodoActual + " oportunidades en el periodo actual (máximo "
                            + MAX_OPORTUNIDADES_POR_PERIODO + ")",
                    false, false, (int) enPeriodoActual, contarPeriodosDistintos(historialCompleto));
        }

        List<Integer> periodosDistintos = historialCompleto.stream()
                .map(r -> r.getPeriodoTitulacion().getIdPeriodo())
                .distinct().toList();
        boolean esPrimeraOcasion = periodosDistintos.isEmpty()
                || (periodosDistintos.size() == 1 && periodosDistintos.contains(periodoActivo.getIdPeriodo()));

        // b) El SUPLETORIO solo está disponible en la primera ocasión
        if (tipoOportunidad == TipoOportunidad.SUPLETORIO && !esPrimeraOcasion) {
            return new ElegibilidadDto(false,
                    "El supletorio solo está disponible en la primera ocasión (primer periodo de titulación)",
                    false, false, (int) enPeriodoActual, periodosDistintos.size());
        }

        // c) Máximo 2 periodos de titulación sin autorización especial
        boolean esPeriodoNuevo = !periodosDistintos.contains(periodoActivo.getIdPeriodo());
        int periodosTrasEsteIntento = esPeriodoNuevo ? periodosDistintos.size() + 1 : periodosDistintos.size();
        if (periodosTrasEsteIntento > MAX_PERIODOS_SIN_AUTORIZACION) {
            boolean autorizado = autorizacionRepo.existsByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(
                    idEstudiante, periodoActivo.getIdPeriodo());
            if (!autorizado) {
                return new ElegibilidadDto(false,
                        "Este sería el periodo de titulación número " + periodosTrasEsteIntento
                                + "; se requiere autorización especial (resolución de tercera matrícula)",
                        false, true, (int) enPeriodoActual, periodosDistintos.size());
            }
        }

        // d) Reprobación en 2da/3ra ocasión exige actualización de conocimientos
        //    e) Plazo de 5 años desde el último periodo también la exige
        if (esPeriodoNuevo && !periodosDistintos.isEmpty()) {
            RegistroOportunidad ultimoDeOtroPeriodo = historialCompleto.get(historialCompleto.size() - 1);
            boolean ultimoFueReprobado = ultimoDeOtroPeriodo.getResultado() == ResultadoOportunidad.REPROBADO
                    || ultimoDeOtroPeriodo.getResultado() == ResultadoOportunidad.NO_PRESENTADO;

            PeriodoTitulacion ultimoPeriodo = ultimoDeOtroPeriodo.getPeriodoTitulacion();
            boolean plazoVencido = ultimoPeriodo.getFechaFin() != null
                    && ultimoPeriodo.getFechaFin().isBefore(LocalDate.now().minusYears(ANIOS_PLAZO_REINGRESO));

            if (ultimoFueReprobado || plazoVencido) {
                boolean tieneActualizacionAprobada = actualizacionRepo
                        .findFirstByEstudiante_IdEstudianteAndEstadoOrderByFechaSolicitudDesc(
                                idEstudiante, EstadoActualizacionConocimientos.APROBADA)
                        .isPresent();
                if (!tieneActualizacionAprobada) {
                    String motivo = plazoVencido
                            ? "Han pasado más de " + ANIOS_PLAZO_REINGRESO
                                    + " años desde el último periodo de titulación; se requiere actualización de conocimientos"
                            : "El estudiante reprobó en una ocasión anterior; se requiere actualización de conocimientos aprobada";
                    return new ElegibilidadDto(false, motivo, true, false,
                            (int) enPeriodoActual, periodosDistintos.size());
                }
            }
        }

        return new ElegibilidadDto(true, null, false, false, (int) enPeriodoActual, periodosDistintos.size());
    }

    private int contarPeriodosDistintos(List<RegistroOportunidad> historial) {
        return (int) historial.stream().map(r -> r.getPeriodoTitulacion().getIdPeriodo()).distinct().count();
    }

    // ═══════════════════════════════════════════════════════════════
    // Autorización de tercera matrícula
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "AutorizacionTerceraMatricula", accion = "AUTORIZAR")
    public void autorizarTerceraMatricula(AutorizarTerceraMatriculaRequest req) {
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));
        PeriodoTitulacion periodo = getPeriodoActivo();
        if (req.resolucion() == null || req.resolucion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La resolución es obligatoria");
        }

        Usuario autoriza = usuarioRepo.findById(req.idUsuarioAutoriza())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO"));

        AutorizacionTerceraMatricula autorizacion = new AutorizacionTerceraMatricula();
        autorizacion.setEstudiante(estudiante);
        autorizacion.setPeriodoTitulacion(periodo);
        autorizacion.setResolucion(req.resolucion());
        autorizacion.setAutorizadoPor(autoriza);
        autorizacionRepo.save(autorizacion);
    }

    private PeriodoTitulacion getPeriodoActivo() {
        return periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));
    }

    private RegistroOportunidadDto toDto(RegistroOportunidad r) {
        Estudiante est = r.getEstudiante();
        return new RegistroOportunidadDto(
                r.getId(),
                est.getIdEstudiante(),
                est.getUsuario() != null ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null,
                r.getPeriodoTitulacion().getIdPeriodo(),
                r.getPeriodoTitulacion().getDescripcion(),
                r.getModalidad().name(),
                r.getTipoOportunidad().name(),
                r.getResultado().name(),
                r.getCalificacion(),
                r.getFecha(),
                r.getObservaciones()
        );
    }
}
