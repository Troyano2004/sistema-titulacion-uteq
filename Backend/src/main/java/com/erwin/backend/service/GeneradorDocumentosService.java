package com.erwin.backend.service;

import com.erwin.backend.entities.*;
import com.erwin.backend.enums.TipoTicPlantilla;
import com.erwin.backend.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Módulo 16 — generación de las 11 plantillas documentales del TIC como PDF
 * pre-llenado (TAREA 2). Reutiliza los datos ya vigentes del pipeline de
 * titulación (ProyectoTitulacion/TribunalProyecto/Sustentacion/...), NO el
 * modelo nuevo de DocumentoTIC del editor por secciones — son documentos
 * oficiales distintos con distinta fuente de datos.
 */
@Service
@Transactional(readOnly = true)
public class GeneradorDocumentosService {

    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern(
            "dd 'de' MMMM 'de' yyyy", new Locale("es", "EC"));
    private static final BigDecimal UMBRAL_ANTIPLAGIO = new BigDecimal("10.00");
    private static final BigDecimal NOTA_APROBACION = new BigDecimal("7.00");

    private final ProyectoTitulacionRepository proyectoRepo;
    private final TribunalProyectoRepository tribunalRepo;
    private final SustentacionRepository sustentacionRepo;
    private final EvaluacionSustentacionRepository evaluacionRepo;
    private final AntiplacioIntentoRepository antiplagioRepo;
    private final ActaGradoRepository actaGradoRepo;

    public GeneradorDocumentosService(ProyectoTitulacionRepository proyectoRepo,
                                       TribunalProyectoRepository tribunalRepo,
                                       SustentacionRepository sustentacionRepo,
                                       EvaluacionSustentacionRepository evaluacionRepo,
                                       AntiplacioIntentoRepository antiplagioRepo,
                                       ActaGradoRepository actaGradoRepo) {
        this.proyectoRepo = proyectoRepo;
        this.tribunalRepo = tribunalRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.evaluacionRepo = evaluacionRepo;
        this.antiplagioRepo = antiplagioRepo;
        this.actaGradoRepo = actaGradoRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    // Resolución de datos compartida
    // ═══════════════════════════════════════════════════════════════
    private record DatosTic(
            ProyectoTitulacion proyecto, Estudiante estudiante, Carrera carrera, Facultad facultad,
            Universidad universidad, Docente director, TipoTicPlantilla tipoPlantilla, String terminoDirector) {}

    private DatosTic resolverDatos(Integer idEstudiante) {
        ProyectoTitulacion proyecto = proyectoRepo
                .findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró un proyecto de titulación para el estudiante " + idEstudiante));

        Estudiante estudiante = proyecto.getPropuesta().getEstudiante();
        Carrera carrera = estudiante.getCarrera();
        Facultad facultad = carrera != null ? carrera.getFacultad() : null;
        Universidad universidad = facultad != null ? facultad.getUniversidad() : null;
        Docente director = proyecto.getDirector();

        String nombreTipoTrabajo = proyecto.getTipoTrabajo() != null ? proyecto.getTipoTrabajo().getNombre() : "";
        TipoTicPlantilla tipoPlantilla = SeccionPlantillaProvider.clasificar(nombreTipoTrabajo);
        String terminoDirector = terminoDirector(tipoPlantilla);

        return new DatosTic(proyecto, estudiante, carrera, facultad, universidad, director, tipoPlantilla, terminoDirector);
    }

    /** TAREA 2.2: la terminología del director cambia según el tipo de TIC. */
    private String terminoDirector(TipoTicPlantilla tipo) {
        return switch (tipo) {
            case PROYECTO_INVESTIGACION -> "Director del Proyecto de Investigación";
            case PROYECTO_TECNOLOGICO -> "Director del Proyecto Tecnológico";
            case ESTUDIO_DE_CASO, ARTICULO_CIENTIFICO -> "Director del Trabajo de Integración Curricular";
        };
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. PORTADA
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarPortada(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        return construir("Portada", doc -> {
            encabezadoInstitucional(doc, d);
            doc.add(espaciado(40));
            doc.add(parrafo(n(d.proyecto().getTitulo()), FUENTE_TITULO, Element.ALIGN_CENTER));
            doc.add(espaciado(10));
            doc.add(parrafo("Trabajo de titulación previo a la obtención del título de " +
                    (d.carrera() != null ? d.carrera().getNombre() : ""), FUENTE_NORMAL, Element.ALIGN_CENTER));
            doc.add(espaciado(30));
            doc.add(parrafo("AUTOR(A):", FUENTE_ETIQUETA, Element.ALIGN_CENTER));
            doc.add(parrafo(nombreCompleto(d.estudiante().getUsuario()), FUENTE_NORMAL, Element.ALIGN_CENTER));
            doc.add(espaciado(16));
            doc.add(parrafo(d.terminoDirector().toUpperCase() + ":", FUENTE_ETIQUETA, Element.ALIGN_CENTER));
            doc.add(parrafo(d.director() != null ? nombreCompleto(d.director().getUsuario()) : "(sin director asignado)",
                    FUENTE_NORMAL, Element.ALIGN_CENTER));
            doc.add(espaciado(40));
            doc.add(parrafo("Quevedo - Los Ríos - Ecuador", FUENTE_NORMAL, Element.ALIGN_CENTER));
            doc.add(parrafo(String.valueOf(LocalDate.now().getYear()), FUENTE_NORMAL, Element.ALIGN_CENTER));
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. DECLARACION_AUTORIA
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarDeclaracionAutoria(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        String nombre = nombreCompleto(d.estudiante().getUsuario());
        String cedula = d.estudiante().getUsuario().getCedula();

        return construir("Declaración de Autoría", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "DECLARACIÓN DE AUTORÍA Y CESIÓN DE DERECHOS");
            doc.add(parrafoJustificado(
                    "Yo, " + nombre + ", portador(a) de la cédula de ciudadanía N.° " + n(cedula) +
                    ", declaro bajo juramento que el trabajo de titulación titulado \"" + n(d.proyecto().getTitulo()) +
                    "\" es de mi autoría exclusiva, elaborado a partir de la investigación, análisis y desarrollo " +
                    "realizado personalmente, respetando los derechos intelectuales de terceros cuyas fuentes se " +
                    "citan debidamente en el presente trabajo.", FUENTE_NORMAL));
            doc.add(espaciado(10));
            doc.add(parrafoJustificado(
                    "Asimismo, cedo mis derechos de propiedad intelectual del presente trabajo a la Universidad " +
                    "Técnica Estatal de Quevedo, según lo establecido en la Ley de Propiedad Intelectual, su " +
                    "reglamento y demás normativa institucional vigente.", FUENTE_NORMAL));
            firmaUnica(doc, nombre, "AUTOR(A)", cedula);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. CERTIFICACION_CULMINACION (Director)
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarCertificacionCulminacion(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        String nombreDirector = d.director() != null ? nombreCompleto(d.director().getUsuario()) : "(sin director asignado)";
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Certificación de Culminación", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "CERTIFICACIÓN DE CULMINACIÓN");
            doc.add(parrafoJustificado(
                    "Quien suscribe, " + nombreDirector + ", en calidad de " + d.terminoDirector() +
                    " \"" + n(d.proyecto().getTitulo()) + "\", elaborado por el/la estudiante " + nombreEstudiante +
                    " de la carrera de " + (d.carrera() != null ? d.carrera().getNombre() : "") +
                    ", CERTIFICO que dicho trabajo ha sido concluido en su totalidad, cumpliendo con los " +
                    "requisitos técnicos, metodológicos y científicos establecidos por la normativa institucional, " +
                    "por lo que autorizo continuar con el proceso de titulación.", FUENTE_NORMAL));
            firmaUnica(doc, nombreDirector, d.terminoDirector().toUpperCase(), null);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. CERTIFICADO_ANTIPLAGIO
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarCertificadoAntiplagio(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        Optional<AntiplacioIntento> ultimo = antiplagioRepo
                .findFirstByProyecto_IdProyectoAndFavorableTrueOrderByFechaIntentoDesc(d.proyecto().getIdProyecto());
        if (ultimo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante no tiene un resultado antiplagio favorable (< 10%) registrado todavía");
        }
        AntiplacioIntento intento = ultimo.get();
        String nombreDirector = d.director() != null ? nombreCompleto(d.director().getUsuario()) : "(sin director asignado)";

        return construir("Certificado Antiplagio", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "CERTIFICADO ANTIPLAGIO — COMPILATIO");
            doc.add(parrafoJustificado(
                    "Quien suscribe, " + nombreDirector + ", en calidad de " + d.terminoDirector() +
                    " \"" + n(d.proyecto().getTitulo()) + "\", CERTIFICO que el documento fue analizado mediante " +
                    "el sistema antiplagio COMPILATIO, obteniendo un porcentaje de coincidencia del " +
                    intento.getPorcentajeCoincidencia() + "%, valor que se encuentra dentro del umbral máximo " +
                    "permitido del " + UMBRAL_ANTIPLAGIO + "% establecido en el Art. 57, numeral 2 del Reglamento.",
                    FUENTE_NORMAL));
            firmaUnica(doc, nombreDirector, d.terminoDirector().toUpperCase(), null);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. PAGINA_TRIBUNAL
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarPaginaTribunal(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        List<TribunalProyecto> tribunal = tribunalRepo.findByProyecto_IdProyecto(d.proyecto().getIdProyecto());

        return construir("Página del Tribunal", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "TRIBUNAL DE SUSTENTACIÓN");
            doc.add(parrafoJustificado("Trabajo de titulación: \"" + n(d.proyecto().getTitulo()) + "\"", FUENTE_NORMAL));
            doc.add(espaciado(20));

            for (String cargo : List.of("PRESIDENTE", "VOCAL", "SUPLENTE")) {
                TribunalProyecto miembro = tribunal.stream()
                        .filter(t -> cargo.equalsIgnoreCase(t.getCargo())).findFirst().orElse(null);
                String nombre = miembro != null ? nombreCompleto(miembro.getDocente().getUsuario()) : "(sin asignar)";
                firmaUnica(doc, nombre, cargo + " DEL TRIBUNAL", null);
                doc.add(espaciado(14));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. CODIGO_DUBLIN
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarCodigoDublin(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Código Dublín", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "CÓDIGO DUBLÍN CORE");

            PdfPTable tabla = new PdfPTable(new float[]{30f, 70f});
            tabla.setWidthPercentage(100);
            filaTabla(tabla, "Título", n(d.proyecto().getTitulo()));
            filaTabla(tabla, "Autor", nombreEstudiante);
            filaTabla(tabla, "Palabras clave", "(a completar por el autor)");
            filaTabla(tabla, "Fecha", LocalDate.now().toString());
            filaTabla(tabla, "Editorial", "Quevedo-UTEQ, " + LocalDate.now().getYear());
            filaTabla(tabla, "Resumen", "(tomado de la sección Resumen del documento)");
            filaTabla(tabla, "Abstract", "(tomado de la sección Abstract del documento)");
            filaTabla(tabla, "Descripción", n(d.proyecto().getTitulo()));
            filaTabla(tabla, "URI", "(se asigna al publicar en el repositorio institucional)");
            doc.add(tabla);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. INFORME_FAVORABLE_DIRECTOR
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarInformeFavorableDirector(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        String nombreDirector = d.director() != null ? nombreCompleto(d.director().getUsuario()) : "(sin director asignado)";
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Informe Favorable del Director", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "INFORME FAVORABLE DEL DIRECTOR");
            doc.add(parrafoJustificado(
                    "Quien suscribe, " + nombreDirector + ", en calidad de " + d.terminoDirector() +
                    " \"" + n(d.proyecto().getTitulo()) + "\" desarrollado por " + nombreEstudiante +
                    ", en cumplimiento de los Artículos 52, 54, 57 y 59 del Reglamento de Régimen Académico, " +
                    "informo de manera FAVORABLE que el trabajo cumple con el rigor científico, metodológico y " +
                    "ético requerido, encontrándose en condiciones de continuar con las etapas de predefensa y " +
                    "sustentación final.", FUENTE_NORMAL));
            firmaUnica(doc, nombreDirector, d.terminoDirector().toUpperCase(), null);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. CERTIFICACION_TRIBUNAL
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarCertificacionTribunal(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        List<TribunalProyecto> tribunal = tribunalRepo.findByProyecto_IdProyecto(d.proyecto().getIdProyecto());
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Certificación del Tribunal", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "INFORME DE CONSENSO DEL TRIBUNAL");
            doc.add(parrafoJustificado("Señor/a Decano/a de la " + (d.facultad() != null ? d.facultad().getNombre() : "Facultad") + ":",
                    FUENTE_NORMAL));
            doc.add(espaciado(6));
            doc.add(parrafoJustificado(
                    "Los miembros del Tribunal de Sustentación designados para la revisión del trabajo de " +
                    "titulación \"" + n(d.proyecto().getTitulo()) + "\", presentado por " + nombreEstudiante +
                    ", una vez analizado el documento de manera individual, MANIFESTAMOS DE CONSENSO nuestro " +
                    "informe FAVORABLE para que el estudiante continúe con el proceso de sustentación final.",
                    FUENTE_NORMAL));
            doc.add(espaciado(20));
            for (TribunalProyecto miembro : tribunal) {
                firmaUnica(doc, nombreCompleto(miembro.getDocente().getUsuario()),
                        n(miembro.getCargo()).toUpperCase() + " DEL TRIBUNAL", null);
                doc.add(espaciado(10));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. CERTIFICADO_REDACCION_TECNICA
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarCertificadoRedaccionTecnica(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        List<TribunalProyecto> tribunal = tribunalRepo.findByProyecto_IdProyecto(d.proyecto().getIdProyecto());
        TribunalProyecto presidente = tribunal.stream()
                .filter(t -> "PRESIDENTE".equalsIgnoreCase(t.getCargo())).findFirst().orElse(null);
        String nombrePresidente = presidente != null ? nombreCompleto(presidente.getDocente().getUsuario()) : "(sin asignar)";
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Certificado de Redacción Técnica", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "CERTIFICADO DE REDACCIÓN TÉCNICA");
            doc.add(parrafoJustificado(
                    "Quien suscribe, " + nombrePresidente + ", en calidad de Presidente del Tribunal de " +
                    "Sustentación del trabajo de titulación \"" + n(d.proyecto().getTitulo()) + "\", presentado por " +
                    nombreEstudiante + ", en cumplimiento del Art. 59 del Reglamento, CERTIFICO que el documento " +
                    "final cumple con las normas de redacción técnica, ortográfica y de citación exigidas para " +
                    "trabajos de titulación.", FUENTE_NORMAL));
            firmaUnica(doc, nombrePresidente, "PRESIDENTE DEL TRIBUNAL", null);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. ACTA_SUSTENTACION
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarActaSustentacion(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        Sustentacion sustentacion = sustentacionRepo
                .findByProyecto_IdProyectoOrderByFechaDescHoraDesc(d.proyecto().getIdProyecto())
                .stream().filter(s -> "DEFENSA_FINAL".equalsIgnoreCase(s.getTipo()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El estudiante todavía no tiene una sustentación final programada"));

        List<EvaluacionSustentacion> evaluaciones = evaluacionRepo
                .findBySustentacion_IdSustentacionAndTipo(sustentacion.getIdSustentacion(), "SUSTENTACION");
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Acta de Sustentación", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "ACTA DE SUSTENTACIÓN");

            PdfPTable datos = new PdfPTable(new float[]{35f, 65f});
            datos.setWidthPercentage(100);
            filaTabla(datos, "Estudiante", nombreEstudiante);
            filaTabla(datos, "Trabajo de titulación", n(d.proyecto().getTitulo()));
            filaTabla(datos, "Fecha", sustentacion.getFecha() != null ? sustentacion.getFecha().toString() : "-");
            filaTabla(datos, "Hora", sustentacion.getHora() != null ? sustentacion.getHora().toString() : "-");
            filaTabla(datos, "Lugar", n(sustentacion.getLugar()));
            doc.add(datos);
            doc.add(espaciado(14));

            doc.add(parrafo("RÚBRICA DE EVALUACIÓN (4 indicadores)", FUENTE_SECCION, Element.ALIGN_LEFT));
            PdfPTable tRubrica = new PdfPTable(new float[]{28f, 15f, 15f, 15f, 15f, 12f});
            tRubrica.setWidthPercentage(100);
            for (String h : new String[]{"Docente", "Calidad (20%)", "Originalidad (20%)", "Dominio (30%)", "Preguntas (30%)", "Nota"}) {
                encabezadoCelda(tRubrica, h);
            }
            BigDecimal sumaNotas = BigDecimal.ZERO;
            for (EvaluacionSustentacion e : evaluaciones) {
                tRubrica.addCell(celda(nombreCompleto(e.getDocente().getUsuario())));
                tRubrica.addCell(celda(valor(e.getCalidadTrabajo())));
                tRubrica.addCell(celda(valor(e.getOriginalidad())));
                tRubrica.addCell(celda(valor(e.getDominioTema())));
                tRubrica.addCell(celda(valor(e.getPreguntas())));
                tRubrica.addCell(celda(valor(e.getNotaFinal())));
                if (e.getNotaFinal() != null) sumaNotas = sumaNotas.add(e.getNotaFinal());
            }
            doc.add(tRubrica);
            doc.add(espaciado(10));

            BigDecimal promedio = evaluaciones.isEmpty() ? null
                    : sumaNotas.divide(BigDecimal.valueOf(evaluaciones.size()), 2, java.math.RoundingMode.HALF_UP);
            String resultado = promedio != null
                    ? (promedio.compareTo(NOTA_APROBACION) >= 0 ? "APROBADO" : "REPROBADO") : "PENDIENTE";
            doc.add(parrafo("Nota final promedio: " + (promedio != null ? promedio.toString() : "Pendiente")
                    + "  —  Resultado: " + resultado, FUENTE_ETIQUETA, Element.ALIGN_LEFT));
            doc.add(espaciado(24));

            for (EvaluacionSustentacion e : evaluaciones) {
                firmaUnica(doc, nombreCompleto(e.getDocente().getUsuario()), "MIEMBRO DEL TRIBUNAL", null);
                doc.add(espaciado(10));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 11. ACTA_GRADO
    // ═══════════════════════════════════════════════════════════════
    public byte[] generarActaGrado(Integer idEstudiante) {
        DatosTic d = resolverDatos(idEstudiante);
        ActaGrado acta = actaGradoRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El estudiante todavía no tiene una nota de grado registrada"));
        String nombreEstudiante = nombreCompleto(d.estudiante().getUsuario());

        return construir("Acta de Grado", doc -> {
            encabezadoInstitucional(doc, d);
            tituloDocumento(doc, "ACTA DE GRADO — CONSEJO ACADÉMICO");
            doc.add(parrafoJustificado(
                    "El Consejo Académico de la " + (d.facultad() != null ? d.facultad().getNombre() : "Facultad") +
                    " de la Universidad Técnica Estatal de Quevedo, en uso de sus atribuciones, deja constancia " +
                    "de la calificación final obtenida por el/la estudiante " + nombreEstudiante +
                    " en su proceso de titulación (" + n(acta.getTipoTitulacion()) + ").", FUENTE_NORMAL));
            doc.add(espaciado(14));

            PdfPTable tabla = new PdfPTable(new float[]{50f, 50f});
            tabla.setWidthPercentage(100);
            filaTabla(tabla, "Promedio del récord académico (80%)", valor(acta.getNotaRecord()));
            filaTabla(tabla, "Nota de titulación (20%)", valor(acta.getNotaTitulacion()));
            filaTabla(tabla, "NOTA FINAL", valor(acta.getNotaFinal()));
            filaTabla(tabla, "Fecha de emisión", acta.getFechaEmision() != null ? acta.getFechaEmision().toString() : "-");
            doc.add(tabla);
            doc.add(espaciado(30));

            firmaUnica(doc, "", "DECANO/A", null);
            doc.add(espaciado(14));
            firmaUnica(doc, "", "SECRETARIO/A ACADÉMICO", null);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers de construcción PDF (estilo institucional, ver ReporteController)
    // ═══════════════════════════════════════════════════════════════
    private static final Font FUENTE_UNIV = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(0, 100, 0));
    private static final Font FUENTE_FACULTAD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(40, 40, 40));
    private static final Font FUENTE_CARRERA = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(80, 80, 80));
    private static final Font FUENTE_TITULO_DOC = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(30, 30, 30));
    private static final Font FUENTE_TITULO = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, new BaseColor(20, 20, 20));
    private static final Font FUENTE_NORMAL = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(30, 30, 30));
    private static final Font FUENTE_ETIQUETA = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(30, 30, 30));
    private static final Font FUENTE_SECCION = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(0, 100, 0));
    private static final Font FUENTE_CELDA_HEADER = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
    private static final Font FUENTE_CELDA = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    private interface CuerpoDocumento { void construir(Document doc) throws Exception; }

    private byte[] construir(String tituloArchivo, CuerpoDocumento cuerpo) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();
            cuerpo.construir(doc);
            doc.close();
            return out.toByteArray();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo generar el documento '" + tituloArchivo + "': " + ex.getMessage(), ex);
        }
    }

    private void encabezadoInstitucional(Document doc, DatosTic d) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO", FUENTE_UNIV));
        p.add(Chunk.NEWLINE);
        if (d.facultad() != null) {
            p.add(new Chunk(d.facultad().getNombre().toUpperCase(), FUENTE_FACULTAD));
            p.add(Chunk.NEWLINE);
        }
        if (d.carrera() != null) {
            p.add(new Chunk("CARRERA DE " + d.carrera().getNombre().toUpperCase(), FUENTE_CARRERA));
        }
        doc.add(p);

        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingBefore(8);
        linea.setSpacingAfter(4);
        PdfPCell lineaCell = new PdfPCell(new Phrase(" "));
        lineaCell.setBorderWidthBottom(2f);
        lineaCell.setBorderColorBottom(new BaseColor(0, 120, 0));
        lineaCell.setBorderWidthTop(0); lineaCell.setBorderWidthLeft(0); lineaCell.setBorderWidthRight(0);
        linea.addCell(lineaCell);
        doc.add(linea);
    }

    private void tituloDocumento(Document doc, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, FUENTE_TITULO_DOC);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(16);
        p.setSpacingAfter(18);
        doc.add(p);
    }

    private Paragraph parrafo(String texto, Font fuente, int alineacion) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(alineacion);
        return p;
    }

    private Paragraph parrafoJustificado(String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setLeading(16);
        p.setSpacingAfter(8);
        return p;
    }

    private Paragraph espaciado(float puntos) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(puntos);
        return p;
    }

    private void filaTabla(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, FUENTE_ETIQUETA));
        c1.setBackgroundColor(new BaseColor(248, 248, 248));
        c1.setBorderColor(new BaseColor(210, 210, 210));
        c1.setPadding(6);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "-", FUENTE_NORMAL));
        c2.setBorderColor(new BaseColor(210, 210, 210));
        c2.setPadding(6);
        tabla.addCell(c2);
    }

    private void encabezadoCelda(PdfPTable tabla, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FUENTE_CELDA_HEADER));
        c.setBackgroundColor(new BaseColor(0, 100, 0));
        c.setPadding(5);
        tabla.addCell(c);
    }

    private PdfPCell celda(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, FUENTE_CELDA));
        c.setBorderColor(new BaseColor(210, 210, 210));
        c.setPadding(5);
        return c;
    }

    private void firmaUnica(Document doc, String nombre, String cargo, String cedula) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(24);

        PdfPCell linea = new PdfPCell(new Phrase(" "));
        linea.setBorderWidthTop(1f);
        linea.setBorderWidthBottom(0); linea.setBorderWidthLeft(0); linea.setBorderWidthRight(0);
        linea.setBorderColorTop(new BaseColor(60, 60, 60));
        t.addCell(linea);

        PdfPCell cNombre = new PdfPCell(new Phrase(nombre != null && !nombre.isBlank() ? nombre : " ", FUENTE_ETIQUETA));
        cNombre.setBorder(Rectangle.NO_BORDER);
        cNombre.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cNombre);

        PdfPCell cCargo = new PdfPCell(new Phrase(cargo, FUENTE_CARRERA));
        cCargo.setBorder(Rectangle.NO_BORDER);
        cCargo.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cCargo);

        if (cedula != null && !cedula.isBlank()) {
            PdfPCell cCedula = new PdfPCell(new Phrase("C.I. " + cedula, FUENTE_CARRERA));
            cCedula.setBorder(Rectangle.NO_BORDER);
            cCedula.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(cCedula);
        }
        doc.add(t);
    }

    private String n(String s) { return s == null ? "" : s; }
    private String valor(BigDecimal b) { return b == null ? "-" : b.toString(); }
}
