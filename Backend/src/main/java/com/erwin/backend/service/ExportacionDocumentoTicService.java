package com.erwin.backend.service;

import com.erwin.backend.entities.DocumentoTIC;
import com.erwin.backend.entities.SeccionDocumentoTIC;
import com.erwin.backend.repository.DocumentoTicRepository;
import com.erwin.backend.repository.SeccionDocumentoTicRepository;
import com.erwin.backend.security.AuthorizationService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exportación PDF/DOCX del documento del TIC con formato institucional UTEQ
 * (TAREA 3): márgenes 3/2.5/2.5/2.5 cm, Times New Roman 12pt, interlineado
 * 1.5, numeración de páginas centrada al pie, portada sin numerar.
 */
@Service
@Transactional(readOnly = true)
public class ExportacionDocumentoTicService {

    private final DocumentoTicRepository documentoRepo;
    private final SeccionDocumentoTicRepository seccionRepo;
    private final AuthorizationService authorizationService;

    public ExportacionDocumentoTicService(DocumentoTicRepository documentoRepo, SeccionDocumentoTicRepository seccionRepo,
                                           AuthorizationService authorizationService) {
        this.documentoRepo = documentoRepo;
        this.seccionRepo = seccionRepo;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // PDF (OpenHTMLtoPDF)
    // ═══════════════════════════════════════════════════════════════
    public byte[] exportarPdf(Long idDocumento) {
        DocumentoTIC documento = getDocumento(idDocumento);
        List<SeccionDocumentoTIC> secciones = seccionRepo.findByDocumento_IdOrderByOrdenAsc(idDocumento);

        String html = construirHtmlInstitucional(documento, secciones);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF del documento", e);
        }
    }

    private String construirHtmlInstitucional(DocumentoTIC documento, List<SeccionDocumentoTIC> secciones) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"><style>")
                .append("@page { size: A4; margin-top: 2.5cm; margin-bottom: 2.5cm; margin-left: 3cm; margin-right: 2.5cm; ")
                .append("  @bottom-center { content: counter(page); font-family: 'Times New Roman'; font-size: 11pt; } }")
                .append("@page :first { @bottom-center { content: \"\"; } }") // portada sin numeración
                .append("body { font-family: 'Times New Roman', serif; font-size: 12pt; line-height: 1.5; text-align: justify; }")
                .append("h1, h2, h3 { font-family: 'Times New Roman', serif; }")
                .append(".seccion { page-break-before: always; }")
                .append(".seccion:first-child { page-break-before: avoid; }")
                .append("table { border-collapse: collapse; width: 100%; }")
                .append("td, th { border: 1px solid #666; padding: 4px; }")
                .append("</style></head><body>");

        for (SeccionDocumentoTIC seccion : secciones) {
            html.append("<div class=\"seccion\">");
            if (!"PORTADA".equals(seccion.getCodigoSeccion())) {
                html.append("<h2>").append(escapar(seccion.getTitulo())).append("</h2>");
            }
            html.append(seccion.getContenidoHtml() != null ? seccion.getContenidoHtml() : "<p><em>(sección vacía)</em></p>");
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ═══════════════════════════════════════════════════════════════
    // DOCX (Apache POI)
    // ═══════════════════════════════════════════════════════════════
    private static final Pattern BLOQUE = Pattern.compile(
            "<(p|li|h[1-6]|div)[^>]*>(.*?)</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int DXA_POR_CM = 567; // 1 cm ≈ 566.93 dxa (veinteavos de punto)

    public byte[] exportarDocx(Long idDocumento) {
        DocumentoTIC documento = getDocumento(idDocumento);
        List<SeccionDocumentoTIC> secciones = seccionRepo.findByDocumento_IdOrderByOrdenAsc(idDocumento);

        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            configurarMargenesInstitucionales(docx);
            configurarNumeracionPaginas(docx);

            for (SeccionDocumentoTIC seccion : secciones) {
                if (!"PORTADA".equals(seccion.getCodigoSeccion())) {
                    XWPFParagraph titulo = docx.createParagraph();
                    XWPFRun run = titulo.createRun();
                    run.setText(seccion.getTitulo());
                    run.setBold(true);
                    run.setFontFamily("Times New Roman");
                    run.setFontSize(13);
                }
                agregarContenidoSeccion(docx, seccion.getContenidoHtml());
            }

            docx.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el DOCX del documento", e);
        }
    }

    private void configurarMargenesInstitucionales(XWPFDocument docx) {
        var sectPr = docx.getDocument().getBody().isSetSectPr()
                ? docx.getDocument().getBody().getSectPr()
                : docx.getDocument().getBody().addNewSectPr();
        var pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pgMar.setLeft(BigInteger.valueOf(3 * DXA_POR_CM));
        pgMar.setRight(BigInteger.valueOf((long) (2.5 * DXA_POR_CM)));
        pgMar.setTop(BigInteger.valueOf((long) (2.5 * DXA_POR_CM)));
        pgMar.setBottom(BigInteger.valueOf((long) (2.5 * DXA_POR_CM)));
    }

    private void configurarNumeracionPaginas(XWPFDocument docx) throws Exception {
        XWPFFooter footer = docx.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(11);

        // Campo PAGE — Apache POI no tiene un helper de alto nivel, se inserta el XML del campo.
        org.apache.xmlbeans.XmlCursor cursor = run.getCTR().newCursor();
        cursor.toEndToken();
        cursor.beginElement("w:fldSimple", "http://schemas.openxmlformats.org/wordprocessingml/2006/main");
        cursor.insertAttributeWithValue("instr", "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "PAGE");
        cursor.dispose();
    }

    private void agregarContenidoSeccion(XWPFDocument docx, String contenidoHtml) {
        if (contenidoHtml == null || contenidoHtml.isBlank()) {
            crearParrafo(docx, "(sección vacía)", false);
            return;
        }
        Matcher m = BLOQUE.matcher(contenidoHtml);
        boolean huboBloque = false;
        while (m.find()) {
            huboBloque = true;
            String etiqueta = m.group(1).toLowerCase();
            String texto = limpiarHtml(m.group(2));
            if (texto.isBlank()) continue;
            crearParrafo(docx, "li".equals(etiqueta) ? "•  " + texto : texto, etiqueta.startsWith("h"));
        }
        if (!huboBloque) {
            crearParrafo(docx, limpiarHtml(contenidoHtml), false);
        }
    }

    private void crearParrafo(XWPFDocument docx, String texto, boolean negrita) {
        XWPFParagraph parrafo = docx.createParagraph();
        parrafo.setAlignment(ParagraphAlignment.BOTH); // justificado
        parrafo.setSpacingBetween(1.5);
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);
        run.setBold(negrita);
    }

    private String limpiarHtml(String html) {
        return html.replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .trim();
    }

    private DocumentoTIC getDocumento(Long idDocumento) {
        DocumentoTIC documento = documentoRepo.findById(idDocumento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NO_ENCONTRADO"));
        if (documento.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(documento.getEstudiante().getIdEstudiante());
        }
        return documento;
    }
}
