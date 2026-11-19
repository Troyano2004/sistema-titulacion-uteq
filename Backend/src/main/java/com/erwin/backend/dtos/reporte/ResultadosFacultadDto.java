package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultadosFacultadDto {
    private String facultad;
    private String periodo;
    private LocalDate fechaGeneracion;
    private LocalDate fechaLimitePlazoArt40;
    private Boolean dentroDePlazo;
    private Integer totalEstudiantes;
    private Integer totalAprobados;
    private Integer totalReprobados;
    private Double tasaAprobacionGeneral;
    private List<CarreraResultadoDto> carreras;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CarreraResultadoDto {
        private String carrera;
        private Integer totalEstudiantes;
        private Double tasaAprobacion;
        private List<ModalidadResultadoDto> porModalidad;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ModalidadResultadoDto {
        private String modalidad;
        private Integer total;
        private Integer aprobados;
        private Integer reprobados;
        private BigDecimal promedioCalificacion;
    }
}
