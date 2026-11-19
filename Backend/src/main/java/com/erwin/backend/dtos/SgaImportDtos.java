package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class SgaImportDtos {

    public record ImportacionResultadoDto(
            String tipo,
            String nombreArchivo,
            int totalFilas,
            int filasOk,
            int filasError,
            List<String> errores,
            LocalDateTime fechaImportacion
    ) {}

    public record ImportacionLogDto(
            Long id,
            String tipo,
            String nombreArchivo,
            int totalFilas,
            int filasOk,
            int filasError,
            String importadoPorNombre,
            LocalDateTime fechaImportacion
    ) {}
}
