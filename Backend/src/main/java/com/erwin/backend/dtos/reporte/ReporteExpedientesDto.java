package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReporteExpedientesDto {
    private String carrera;
    private String periodo;
    private Integer totalExpedientes;
    private Integer incompletos;
    private Integer completos;
    private Integer validados;
    private Integer legalizados;
    private List<DocumentoFaltanteDto> documentosMasFaltantes;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentoFaltanteDto {
        private String documento;
        private Long vecesFaltante;
    }
}
