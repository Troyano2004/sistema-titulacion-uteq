package com.erwin.backend.controller;

import com.erwin.backend.dtos.reporte.DashboardReportesDto;
import com.erwin.backend.dtos.reporte.ReporteExpedientesDto;
import com.erwin.backend.dtos.reporte.ResultadosFacultadDto;
import com.erwin.backend.dtos.reporte.TrazabilidadEstudianteDto;
import com.erwin.backend.service.ReportesInstitucionalesService;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Reportes institucionales agregados (Módulo de auditoría y reportes, TAREA 2):
 * resultados por facultad, trazabilidad de estudiante, dashboard general y
 * estado de expedientes por carrera. Controlador nuevo e independiente de
 * ReporteController (que ya cubre expediente individual / reporte por periodo /
 * actas) para no mezclar estilos ni arriesgar esos endpoints existentes.
 */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA')")
public class ReporteInstitucionalController {

    private final ReportesInstitucionalesService svc;

    public ReporteInstitucionalController(ReportesInstitucionalesService svc) {
        this.svc = svc;
    }

    @GetMapping("/resultados-facultad/{facultadId}")
    public ResultadosFacultadDto resultadosFacultad(@PathVariable Integer facultadId,
                                                      @RequestParam String periodo) {
        return svc.getResultadosFacultad(facultadId, periodo);
    }

    @GetMapping("/resultados-facultad/{facultadId}/pdf")
    public void resultadosFacultadPdf(@PathVariable Integer facultadId, @RequestParam String periodo,
                                       HttpServletResponse response) throws Exception {
        ResultadosFacultadDto dto = svc.getResultadosFacultad(facultadId, periodo);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=Resultados_" + dto.getFacultad().replace(" ", "_") + "_" + periodo + ".pdf");

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        Font fUnivNombre = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 100, 0));
        Font fTitDoc      = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(30, 30, 30));
        Font fSubtitulo   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(80, 80, 80));
        Font fSeccion     = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(30, 30, 30));
        Font fLabelCelda  = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(255, 255, 255));
        Font fValorCelda  = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(30, 30, 30));
        Font fAlerta      = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(180, 0, 0));
        Font fOk          = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(0, 100, 0));

        Paragraph pUniv = new Paragraph("Universidad Tecnica Estatal de Quevedo", fUnivNombre);
        doc.add(pUniv);
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100); linea.setSpacingBefore(4); linea.setSpacingAfter(12);
        PdfPCell lineaCell = new PdfPCell(new Phrase(" "));
        lineaCell.setBorderWidthBottom(2f); lineaCell.setBorderColorBottom(new BaseColor(0, 120, 0));
        lineaCell.setBorderWidthTop(0); lineaCell.setBorderWidthLeft(0); lineaCell.setBorderWidthRight(0);
        linea.addCell(lineaCell);
        doc.add(linea);

        doc.add(new Paragraph("Informe de Resultados por Facultad (Art. 40)", fTitDoc));
        doc.add(new Paragraph(dto.getFacultad() + " - Periodo " + dto.getPeriodo(), fSubtitulo));
        String fechaFormateada = LocalDate.now().format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "EC")));
        doc.add(new Paragraph("Fecha de emisión: " + fechaFormateada, fSubtitulo));
        if (dto.getFechaLimitePlazoArt40() != null) {
            boolean ok = Boolean.TRUE.equals(dto.getDentroDePlazo());
            doc.add(new Paragraph(
                "Plazo Art. 40 (8 días hábiles desde el último complexivo): límite " + dto.getFechaLimitePlazoArt40()
                    + (ok ? " — dentro de plazo" : " — FUERA DE PLAZO"),
                ok ? fOk : fAlerta));
        }
        doc.add(espaciado(10));

        doc.add(new Paragraph("Resumen general", fSeccion));
        PdfPTable tResumen = new PdfPTable(new float[]{40f, 20f, 20f, 20f});
        tResumen.setWidthPercentage(100); tResumen.setSpacingBefore(6); tResumen.setSpacingAfter(14);
        encabezadoFila(tResumen, fLabelCelda, "TOTAL ESTUDIANTES", "APROBADOS", "REPROBADOS", "TASA APROBACIÓN");
        agregarCelda(tResumen, String.valueOf(dto.getTotalEstudiantes()), fValorCelda);
        agregarCelda(tResumen, String.valueOf(dto.getTotalAprobados()), fValorCelda);
        agregarCelda(tResumen, String.valueOf(dto.getTotalReprobados()), fValorCelda);
        agregarCelda(tResumen, String.format("%.1f%%", dto.getTasaAprobacionGeneral()), fValorCelda);
        doc.add(tResumen);

        doc.add(new Paragraph("Resultados por carrera y modalidad", fSeccion));
        PdfPTable tCarreras = new PdfPTable(new float[]{28f, 18f, 12f, 12f, 12f, 18f});
        tCarreras.setWidthPercentage(100); tCarreras.setSpacingBefore(6);
        encabezadoFila(tCarreras, fLabelCelda, "CARRERA", "MODALIDAD", "TOTAL", "APROB.", "REPR.", "PROMEDIO");
        for (ResultadosFacultadDto.CarreraResultadoDto c : dto.getCarreras()) {
            for (ResultadosFacultadDto.ModalidadResultadoDto m : c.getPorModalidad()) {
                agregarCelda(tCarreras, c.getCarrera(), fValorCelda);
                agregarCelda(tCarreras, m.getModalidad(), fValorCelda);
                agregarCelda(tCarreras, String.valueOf(m.getTotal()), fValorCelda);
                agregarCelda(tCarreras, String.valueOf(m.getAprobados()), fValorCelda);
                agregarCelda(tCarreras, String.valueOf(m.getReprobados()), fValorCelda);
                agregarCelda(tCarreras, m.getPromedioCalificacion() != null ? m.getPromedioCalificacion().toString() : "-", fValorCelda);
            }
        }
        doc.add(tCarreras);

        doc.close();
    }

    @GetMapping("/trazabilidad/{estudianteId}")
    public TrazabilidadEstudianteDto trazabilidad(@PathVariable Integer estudianteId) {
        return svc.getTrazabilidad(estudianteId);
    }

    @GetMapping("/dashboard")
    public DashboardReportesDto dashboard(@RequestParam String periodo) {
        return svc.getDashboard(periodo);
    }

    @GetMapping("/expedientes")
    public ReporteExpedientesDto expedientes(@RequestParam(required = false) Integer carrera,
                                              @RequestParam(required = false) String periodo) {
        return svc.getReporteExpedientes(carrera, periodo);
    }

    // ── Helpers PDF ───────────────────────────────────────────────────────

    private void encabezadoFila(PdfPTable tabla, Font font, String... encabezados) {
        for (String h : encabezados) {
            PdfPCell c = new PdfPCell(new Phrase(h, font));
            c.setBackgroundColor(new BaseColor(0, 120, 0));
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(c);
        }
    }

    private void agregarCelda(PdfPTable tabla, String valor, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(valor != null ? valor : "-", font));
        c.setPadding(5);
        c.setBorderColor(new BaseColor(200, 200, 200));
        tabla.addCell(c);
    }

    private Paragraph espaciado(float puntos) {
        Paragraph p = new Paragraph(Chunk.NEWLINE);
        p.setSpacingAfter(puntos);
        return p;
    }
}
