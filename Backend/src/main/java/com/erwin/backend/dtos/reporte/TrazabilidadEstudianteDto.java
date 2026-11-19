package com.erwin.backend.dtos.reporte;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrazabilidadEstudianteDto {
    private Integer idEstudiante;
    private String estudiante;
    private String cedula;
    private String carrera;
    private List<EventoTrazabilidadDto> eventos;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EventoTrazabilidadDto {
        private String etapa;
        private LocalDateTime fecha;
        private String actor;
        private String estadoResultante;
        private String detalle;
    }
}
