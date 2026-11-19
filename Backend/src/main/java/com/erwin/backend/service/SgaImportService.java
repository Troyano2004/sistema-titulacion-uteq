package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.SgaImportDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoAsignaturaImportada;
import com.erwin.backend.enums.EstadoDeudaImportada;
import com.erwin.backend.enums.TipoImportacionSga;
import com.erwin.backend.repository.*;
import com.erwin.backend.util.ImportadorArchivoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Módulo 20 (TAREA 2) — importación manual mejorada de datos del SGA.
 * Puente temporal reemplazable: si algún día existe una API real del SGA,
 * basta con sustituir el origen de los datos (este archivo CSV/XLSX) por
 * llamadas a esa API — el resto del sistema (PensumImportado,
 * RequisitoImportado, DeudaImportado y su uso en MatriculaTitulacionService)
 * no cambia.
 */
@Service
@Transactional
public class SgaImportService {

    private final PensumImportadoRepository pensumRepo;
    private final RequisitoImportadoRepository requisitoRepo;
    private final DeudaImportadaRepository deudaRepo;
    private final ImportacionSgaLogRepository logRepo;
    private final EstudianteRepository estudianteRepo;
    private final UsuarioRepository usuarioRepo;

    public SgaImportService(PensumImportadoRepository pensumRepo,
                             RequisitoImportadoRepository requisitoRepo,
                             DeudaImportadaRepository deudaRepo,
                             ImportacionSgaLogRepository logRepo,
                             EstudianteRepository estudianteRepo,
                             UsuarioRepository usuarioRepo) {
        this.pensumRepo = pensumRepo;
        this.requisitoRepo = requisitoRepo;
        this.deudaRepo = deudaRepo;
        this.logRepo = logRepo;
        this.estudianteRepo = estudianteRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    // Pensum: cedulaEstudiante, codigoAsignatura, nombreAsignatura, calificacion, estado, periodoAcademico
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "PensumImportado", accion = "IMPORTAR")
    public ImportacionResultadoDto importarPensum(MultipartFile file, Integer idUsuarioImporta) {
        List<String[]> filas = ImportadorArchivoUtil.leerFilas(file);
        List<String> errores = new ArrayList<>();
        int ok = 0;
        Set<Integer> estudiantesTocados = new LinkedHashSet<>();

        for (int i = 0; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            int numeroFila = i + 2; // +1 por índice 0, +1 por el encabezado
            try {
                String cedula = ImportadorArchivoUtil.celda(fila, 0);
                String codigo = ImportadorArchivoUtil.celda(fila, 1);
                String nombre = ImportadorArchivoUtil.celda(fila, 2);
                String califStr = ImportadorArchivoUtil.celda(fila, 3);
                String estadoStr = ImportadorArchivoUtil.celda(fila, 4);
                String periodo = ImportadorArchivoUtil.celda(fila, 5);

                Estudiante estudiante = resolverEstudiante(cedula);
                if (codigo.isBlank()) throw new IllegalArgumentException("código de asignatura vacío");
                BigDecimal calificacion = parseCalificacion(califStr);
                EstadoAsignaturaImportada estado = EstadoAsignaturaImportada.valueOf(estadoStr.trim().toUpperCase());

                PensumImportado registro = new PensumImportado();
                registro.setEstudiante(estudiante);
                registro.setCodigoAsignatura(codigo);
                registro.setNombreAsignatura(nombre);
                registro.setCalificacion(calificacion);
                registro.setEstado(estado);
                registro.setPeriodoAcademico(periodo);
                pensumRepo.save(registro);

                estudiantesTocados.add(estudiante.getIdEstudiante());
                ok++;
            } catch (Exception ex) {
                errores.add("Fila " + numeroFila + ": " + ex.getMessage());
            }
        }

        for (Integer idEstudiante : estudiantesTocados) {
            recalcularPromedio(idEstudiante);
        }

        return registrarLog(TipoImportacionSga.PENSUM, file, filas.size(), ok, errores, idUsuarioImporta);
    }

    private void recalcularPromedio(Integer idEstudiante) {
        List<PensumImportado> registros = pensumRepo.findByEstudiante_IdEstudiante(idEstudiante);
        List<BigDecimal> notas = registros.stream()
                .map(PensumImportado::getCalificacion)
                .filter(Objects::nonNull)
                .toList();
        if (notas.isEmpty()) return;

        BigDecimal suma = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);

        estudianteRepo.findById(idEstudiante).ifPresent(e -> {
            e.setPromedioRecord80(promedio);
            estudianteRepo.save(e);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // Requisitos: cedulaEstudiante, idiomaAprobado(S/N), practicasLaborales(S/N),
    //             horasPracticas, servicioComunitario(S/N), horasServicio
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "RequisitoImportado", accion = "IMPORTAR")
    public ImportacionResultadoDto importarRequisitos(MultipartFile file, Integer idUsuarioImporta) {
        List<String[]> filas = ImportadorArchivoUtil.leerFilas(file);
        List<String> errores = new ArrayList<>();
        int ok = 0;

        for (int i = 0; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            int numeroFila = i + 2;
            try {
                String cedula = ImportadorArchivoUtil.celda(fila, 0);
                boolean idioma = parseSiNo(ImportadorArchivoUtil.celda(fila, 1));
                boolean practicas = parseSiNo(ImportadorArchivoUtil.celda(fila, 2));
                Integer horasPracticas = parseEnteroOpcional(ImportadorArchivoUtil.celda(fila, 3));
                boolean servicio = parseSiNo(ImportadorArchivoUtil.celda(fila, 4));
                Integer horasServicio = parseEnteroOpcional(ImportadorArchivoUtil.celda(fila, 5));

                Estudiante estudiante = resolverEstudiante(cedula);

                RequisitoImportado registro = requisitoRepo.findByEstudiante_IdEstudiante(estudiante.getIdEstudiante())
                        .orElse(new RequisitoImportado());
                registro.setEstudiante(estudiante);
                registro.setIdiomaAprobado(idioma);
                registro.setPracticasLaborales(practicas);
                registro.setHorasPracticas(horasPracticas);
                registro.setServicioComunitario(servicio);
                registro.setHorasServicio(horasServicio);
                registro.setFechaImportacion(LocalDateTime.now());
                requisitoRepo.save(registro);
                ok++;
            } catch (Exception ex) {
                errores.add("Fila " + numeroFila + ": " + ex.getMessage());
            }
        }

        return registrarLog(TipoImportacionSga.REQUISITOS, file, filas.size(), ok, errores, idUsuarioImporta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Deudas: cedulaEstudiante, tipoDeuda, monto, estado
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "DeudaImportada", accion = "IMPORTAR")
    public ImportacionResultadoDto importarDeudas(MultipartFile file, Integer idUsuarioImporta) {
        List<String[]> filas = ImportadorArchivoUtil.leerFilas(file);
        List<String> errores = new ArrayList<>();
        int ok = 0;

        for (int i = 0; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            int numeroFila = i + 2;
            try {
                String cedula = ImportadorArchivoUtil.celda(fila, 0);
                String tipoDeuda = ImportadorArchivoUtil.celda(fila, 1);
                String montoStr = ImportadorArchivoUtil.celda(fila, 2);
                String estadoStr = ImportadorArchivoUtil.celda(fila, 3);

                Estudiante estudiante = resolverEstudiante(cedula);
                if (tipoDeuda.isBlank()) throw new IllegalArgumentException("tipo de deuda vacío");
                BigDecimal monto = montoStr.isBlank() ? null : new BigDecimal(montoStr.replace(",", "."));
                EstadoDeudaImportada estado = EstadoDeudaImportada.valueOf(estadoStr.trim().toUpperCase());

                DeudaImportada registro = new DeudaImportada();
                registro.setEstudiante(estudiante);
                registro.setTipoDeuda(tipoDeuda);
                registro.setMonto(monto);
                registro.setEstado(estado);
                deudaRepo.save(registro);
                ok++;
            } catch (Exception ex) {
                errores.add("Fila " + numeroFila + ": " + ex.getMessage());
            }
        }

        return registrarLog(TipoImportacionSga.DEUDAS, file, filas.size(), ok, errores, idUsuarioImporta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Historial (TAREA 2.4)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<ImportacionLogDto> historial() {
        return logRepo.findAllByOrderByFechaImportacionDesc().stream()
                .map(this::toLogDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    private Estudiante resolverEstudiante(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("cédula vacía");
        }
        return estudianteRepo.findByUsuario_Cedula(cedula.trim())
                .orElseThrow(() -> new IllegalArgumentException("no existe un estudiante con cédula " + cedula));
    }

    private BigDecimal parseCalificacion(String valor) {
        if (valor == null || valor.isBlank()) return null;
        BigDecimal calificacion;
        try {
            calificacion = new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("calificación inválida: " + valor);
        }
        if (calificacion.compareTo(BigDecimal.ZERO) < 0 || calificacion.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("la calificación debe estar entre 0 y 10 (recibida: " + valor + ")");
        }
        return calificacion;
    }

    private boolean parseSiNo(String valor) {
        String v = valor == null ? "" : valor.trim().toUpperCase();
        if (v.equals("S") || v.equals("SI") || v.equals("SÍ") || v.equals("TRUE") || v.equals("1")) return true;
        if (v.equals("N") || v.equals("NO") || v.equals("FALSE") || v.equals("0") || v.isBlank()) return false;
        throw new IllegalArgumentException("valor S/N inválido: " + valor);
    }

    private Integer parseEnteroOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("número inválido: " + valor);
        }
    }

    private ImportacionResultadoDto registrarLog(TipoImportacionSga tipo, MultipartFile file, int total,
                                                  int ok, List<String> errores, Integer idUsuarioImporta) {
        ImportacionSgaLog log = new ImportacionSgaLog();
        log.setTipo(tipo);
        log.setNombreArchivo(file.getOriginalFilename());
        log.setTotalFilas(total);
        log.setFilasOk(ok);
        log.setFilasError(errores.size());
        log.setErrores(String.join("\n", errores));
        if (idUsuarioImporta != null) {
            usuarioRepo.findById(idUsuarioImporta).ifPresent(log::setImportadoPor);
        }
        logRepo.save(log);

        return new ImportacionResultadoDto(tipo.name(), file.getOriginalFilename(), total, ok, errores.size(),
                errores, log.getFechaImportacion());
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private ImportacionLogDto toLogDto(ImportacionSgaLog l) {
        return new ImportacionLogDto(
                l.getId(), l.getTipo().name(), l.getNombreArchivo(), l.getTotalFilas(), l.getFilasOk(),
                l.getFilasError(), l.getImportadoPor() != null ? nombreCompleto(l.getImportadoPor()) : null,
                l.getFechaImportacion()
        );
    }
}
