package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.DocumentoTicDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstandarBibliografico;
import com.erwin.backend.enums.TipoReferenciaBibliografica;
import com.erwin.backend.repository.DocumentoTicRepository;
import com.erwin.backend.repository.ReferenciaBibliograficaRepository;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bibliografía del TIC (TAREA 1.5). El estándar de formato se toma de
 * {@code Carrera.facultad.estandarBibliografico}; Art. 46: mínimo 10
 * referencias, 60% con antigüedad ≤ 5 años.
 */
@Service
@Transactional
public class BibliografiaService {

    public static final int MINIMO_REFERENCIAS = 10;
    public static final double PORCENTAJE_MINIMO_RECIENTES = 60.0;
    private static final int ANIOS_ANTIGUEDAD_MAXIMA = 5;

    private final ReferenciaBibliograficaRepository referenciaRepo;
    private final DocumentoTicRepository documentoRepo;
    private final AuthorizationService authorizationService;

    public BibliografiaService(ReferenciaBibliograficaRepository referenciaRepo, DocumentoTicRepository documentoRepo,
                                AuthorizationService authorizationService) {
        this.referenciaRepo = referenciaRepo;
        this.documentoRepo = documentoRepo;
        this.authorizationService = authorizationService;
    }

    private DocumentoTIC getDocumento(Long idDocumento) {
        DocumentoTIC documento = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NO_ENCONTRADO"));
        if (documento.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(documento.getEstudiante().getIdEstudiante());
        }
        return documento;
    }

    @Auditable(entidad = "ReferenciaBibliografica", accion = "CREATE")
    public ReferenciaBibliograficaResponse crear(Long idDocumento, ReferenciaBibliograficaRequest req) {
        DocumentoTIC documento = getDocumento(idDocumento);

        validar(req);

        ReferenciaBibliografica ref = new ReferenciaBibliografica();
        ref.setDocumento(documento);
        aplicar(ref, req);
        return toDto(referenciaRepo.save(ref));
    }

    @Auditable(entidad = "ReferenciaBibliografica", accion = "UPDATE")
    public ReferenciaBibliograficaResponse actualizar(Long idDocumento, Long idReferencia, ReferenciaBibliograficaRequest req) {
        getDocumento(idDocumento);
        ReferenciaBibliografica ref = referenciaRepo.findById(idReferencia)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REFERENCIA_NO_ENCONTRADA"));
        if (!ref.getDocumento().getId().equals(idDocumento)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "REFERENCIA_NO_ENCONTRADA");
        }
        validar(req);
        aplicar(ref, req);
        return toDto(referenciaRepo.save(ref));
    }

    public void eliminar(Long idDocumento, Long idReferencia) {
        getDocumento(idDocumento);
        ReferenciaBibliografica ref = referenciaRepo.findById(idReferencia)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REFERENCIA_NO_ENCONTRADA"));
        if (!ref.getDocumento().getId().equals(idDocumento)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "REFERENCIA_NO_ENCONTRADA");
        }
        referenciaRepo.delete(ref);
    }

    @Transactional(readOnly = true)
    public List<ReferenciaBibliograficaResponse> listar(Long idDocumento) {
        getDocumento(idDocumento);
        return referenciaRepo.findByDocumento_Id(idDocumento).stream().map(this::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // Formateo automático + validación Art. 46 (TAREA 1.5)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public BibliografiaFormateadaResponse formatear(Long idDocumento, String estandarParam) {
        DocumentoTIC documento = getDocumento(idDocumento);
        List<ReferenciaBibliografica> referencias = referenciaRepo.findByDocumento_Id(idDocumento);

        EstandarBibliografico estandar = resolverEstandar(estandarParam, documento);

        List<String> formateadas = referencias.stream()
                .map(r -> formatearReferencia(r, estandar))
                .collect(Collectors.toList());

        int total = referencias.size();
        int anioLimite = LocalDate.now().getYear() - ANIOS_ANTIGUEDAD_MAXIMA;
        long recientes = referencias.stream().filter(r -> r.getAnioPublicacion() != null && r.getAnioPublicacion() >= anioLimite).count();
        double porcentajeRecientes = total == 0 ? 0.0 : (recientes * 100.0) / total;

        return new BibliografiaFormateadaResponse(
                estandar.name(),
                formateadas,
                total,
                MINIMO_REFERENCIAS,
                total >= MINIMO_REFERENCIAS,
                Math.round(porcentajeRecientes * 100) / 100.0,
                PORCENTAJE_MINIMO_RECIENTES,
                porcentajeRecientes >= PORCENTAJE_MINIMO_RECIENTES
        );
    }

    private EstandarBibliografico resolverEstandar(String estandarParam, DocumentoTIC documento) {
        if (estandarParam != null && !estandarParam.isBlank()) {
            try {
                return EstandarBibliografico.valueOf(estandarParam.trim().toUpperCase());
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estándar inválido: " + estandarParam);
            }
        }
        Carrera carrera = documento.getEstudiante().getCarrera();
        Facultad facultad = carrera != null ? carrera.getFacultad() : null;
        if (facultad != null && facultad.getEstandarBibliografico() != null) {
            return facultad.getEstandarBibliografico();
        }
        return EstandarBibliografico.APA;
    }

    private String formatearReferencia(ReferenciaBibliografica r, EstandarBibliografico estandar) {
        String autores = r.getAutores();
        String titulo = r.getTitulo();
        Integer anio = r.getAnioPublicacion();
        String fuente = r.getFuente() != null ? r.getFuente() : "";

        return switch (estandar) {
            case APA -> autores + " (" + anio + "). " + titulo + ". " + fuente + ".";
            case IEEE -> "\"" + titulo + "\", " + autores + ", " + fuente + ", " + anio + ".";
            case VANCOUVER -> autores + ". " + titulo + ". " + fuente + ". " + anio + ".";
        };
    }

    /**
     * Valida el Art. 46 sobre una lista de referencias aún no persistidas
     * (usado al crear la propuesta, antes de que exista un DocumentoTIC al
     * que asociarlas vía {@link #crear}).
     */
    public void validarMinimoYRecencia(List<ReferenciaBibliograficaRequest> referencias) {
        int total = referencias == null ? 0 : referencias.size();
        if (total < MINIMO_REFERENCIAS) {
            throw new IllegalArgumentException(
                    "Se requieren mínimo " + MINIMO_REFERENCIAS + " referencias bibliográficas (actualmente: " + total + ")");
        }
        referencias.forEach(this::validar);

        int anioLimite = LocalDate.now().getYear() - ANIOS_ANTIGUEDAD_MAXIMA;
        long recientes = referencias.stream()
                .filter(r -> r.anioPublicacion() != null && r.anioPublicacion() >= anioLimite)
                .count();
        double porcentajeRecientes = (recientes * 100.0) / total;
        if (porcentajeRecientes < PORCENTAJE_MINIMO_RECIENTES) {
            throw new IllegalArgumentException(String.format(
                    "Se requiere que al menos %.0f%% de las referencias tengan una antigüedad de %d años o menos (actualmente: %.1f%%)",
                    PORCENTAJE_MINIMO_RECIENTES, ANIOS_ANTIGUEDAD_MAXIMA, porcentajeRecientes));
        }
    }

    private void validar(ReferenciaBibliograficaRequest req) {
        if (req.autores() == null || req.autores().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los autores son obligatorios");
        }
        if (req.titulo() == null || req.titulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio");
        }
        if (req.anioPublicacion() == null || req.anioPublicacion() < 1900 || req.anioPublicacion() > LocalDate.now().getYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año de publicación no es válido");
        }
        parseTipo(req.tipo());
    }

    private TipoReferenciaBibliografica parseTipo(String tipo) {
        try {
            return TipoReferenciaBibliografica.valueOf(tipo.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de referencia inválido: " + tipo);
        }
    }

    private void aplicar(ReferenciaBibliografica ref, ReferenciaBibliograficaRequest req) {
        ref.setAutores(req.autores());
        ref.setTitulo(req.titulo());
        ref.setAnioPublicacion(req.anioPublicacion());
        ref.setFuente(req.fuente());
        ref.setTipo(parseTipo(req.tipo()));
    }

    private ReferenciaBibliograficaResponse toDto(ReferenciaBibliografica r) {
        return new ReferenciaBibliograficaResponse(r.getId(), r.getAutores(), r.getTitulo(),
                r.getAnioPublicacion(), r.getFuente(), r.getTipo().name());
    }
}
