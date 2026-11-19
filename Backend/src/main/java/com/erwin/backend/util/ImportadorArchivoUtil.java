package com.erwin.backend.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Módulo 20 — lector genérico de CSV/XLSX para la importación manual del SGA
 * (TAREA 2.2). Devuelve filas de celdas de texto, sin la fila de encabezado.
 * Puente temporal: se diseñó como una utilidad aislada y reemplazable el día
 * que exista una API real del SGA (ver REGLAS).
 */
public final class ImportadorArchivoUtil {

    private ImportadorArchivoUtil() {}

    public static List<String[]> leerFilas(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }
        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                return leerXlsx(file.getInputStream());
            }
            return leerCsv(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    private static List<String[]> leerCsv(InputStream is) throws IOException {
        List<String[]> filas = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            String linea;
            boolean primera = true;
            while ((linea = reader.readLine()) != null) {
                if (primera) { primera = false; continue; } // salta encabezado
                if (linea.isBlank()) continue;
                filas.add(parsearLineaCsv(linea));
            }
        }
        return filas;
    }

    /** Parser CSV mínimo: separa por comas respetando campos entre comillas dobles. */
    private static String[] parsearLineaCsv(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                entreComillas = !entreComillas;
            } else if (c == ',' && !entreComillas) {
                celdas.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        celdas.add(actual.toString().trim());
        return celdas.toArray(new String[0]);
    }

    private static List<String[]> leerXlsx(InputStream is) throws IOException {
        List<String[]> filas = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean primera = true;
            for (Row row : sheet) {
                if (primera) { primera = false; continue; } // salta encabezado
                int ultimaCol = row.getLastCellNum();
                if (ultimaCol < 0) continue;
                String[] celdas = new String[ultimaCol];
                boolean vacia = true;
                for (int c = 0; c < ultimaCol; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String valor = formatter.formatCellValue(cell).trim();
                    celdas[c] = valor;
                    if (!valor.isBlank()) vacia = false;
                }
                if (!vacia) filas.add(celdas);
            }
        }
        return filas;
    }

    public static String celda(String[] fila, int indice) {
        return indice < fila.length ? fila[indice].trim() : "";
    }
}
