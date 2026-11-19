package com.erwin.backend.dtos;

import java.util.List;

public class ListadoSeleccionesResponse {

    public static class EstudianteSeleccionDto {
        private Integer idEstudiante;
        private String nombreCompleto;
        private String cedula;
        private String opcionTitulacion; // nombre de la modalidad o "SIN_SELECCIONAR"

        public EstudianteSeleccionDto() {}

        public EstudianteSeleccionDto(Integer idEstudiante, String nombreCompleto, String cedula, String opcionTitulacion) {
            this.idEstudiante = idEstudiante;
            this.nombreCompleto = nombreCompleto;
            this.cedula = cedula;
            this.opcionTitulacion = opcionTitulacion;
        }

        public Integer getIdEstudiante() { return idEstudiante; }
        public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }
        public String getNombreCompleto() { return nombreCompleto; }
        public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
        public String getCedula() { return cedula; }
        public void setCedula(String cedula) { this.cedula = cedula; }
        public String getOpcionTitulacion() { return opcionTitulacion; }
        public void setOpcionTitulacion(String opcionTitulacion) { this.opcionTitulacion = opcionTitulacion; }
    }

    public static class GrupoOpcionDto {
        private String opcionTitulacion;
        private List<EstudianteSeleccionDto> estudiantes;

        public GrupoOpcionDto() {}

        public GrupoOpcionDto(String opcionTitulacion, List<EstudianteSeleccionDto> estudiantes) {
            this.opcionTitulacion = opcionTitulacion;
            this.estudiantes = estudiantes;
        }

        public String getOpcionTitulacion() { return opcionTitulacion; }
        public void setOpcionTitulacion(String opcionTitulacion) { this.opcionTitulacion = opcionTitulacion; }
        public List<EstudianteSeleccionDto> getEstudiantes() { return estudiantes; }
        public void setEstudiantes(List<EstudianteSeleccionDto> estudiantes) { this.estudiantes = estudiantes; }
    }

    private Integer idCarrera;
    private String nombreCarrera;
    private String periodoAcademico;
    private Integer nivelAntepenultimo;
    private List<GrupoOpcionDto> grupos;

    public ListadoSeleccionesResponse() {}

    public ListadoSeleccionesResponse(Integer idCarrera, String nombreCarrera, String periodoAcademico,
                                       Integer nivelAntepenultimo, List<GrupoOpcionDto> grupos) {
        this.idCarrera = idCarrera;
        this.nombreCarrera = nombreCarrera;
        this.periodoAcademico = periodoAcademico;
        this.nivelAntepenultimo = nivelAntepenultimo;
        this.grupos = grupos;
    }

    public Integer getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Integer idCarrera) { this.idCarrera = idCarrera; }
    public String getNombreCarrera() { return nombreCarrera; }
    public void setNombreCarrera(String nombreCarrera) { this.nombreCarrera = nombreCarrera; }
    public String getPeriodoAcademico() { return periodoAcademico; }
    public void setPeriodoAcademico(String periodoAcademico) { this.periodoAcademico = periodoAcademico; }
    public Integer getNivelAntepenultimo() { return nivelAntepenultimo; }
    public void setNivelAntepenultimo(Integer nivelAntepenultimo) { this.nivelAntepenultimo = nivelAntepenultimo; }
    public List<GrupoOpcionDto> getGrupos() { return grupos; }
    public void setGrupos(List<GrupoOpcionDto> grupos) { this.grupos = grupos; }
}
