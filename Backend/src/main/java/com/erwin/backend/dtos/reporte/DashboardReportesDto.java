package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardReportesDto {
    private String periodo;
    private Integer totalMatriculados;
    private List<ConteoDto> matriculadosPorNivel;
    private Integer enProgreso;
    private Integer aprobados;
    private Integer reprobados;
    private Double tasaAprobacion;
    private BigDecimal promedioNotaGrado;
    private Double tiempoPromedioTitulacionDias;
    private List<ModalidadConteoDto> porModalidad;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ConteoDto {
        private String etiqueta;
        private Long cantidad;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ModalidadConteoDto {
        private String modalidad;
        private Long total;
        private Long aprobados;
        private Long reprobados;
        private Long enProceso;
    }
}
