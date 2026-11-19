package com.erwin.backend.dtos;

public class FacultadDto {
    private Integer idFacultad;
    private String nombre;
    private Integer idUniversidad;
    private String nombreUniversidad;
    private Integer idDocenteDecano;
    private String nombreDecano;

    public FacultadDto() {}

    public FacultadDto(Integer idFacultad, String nombre, Integer idUniversidad, String nombreUniversidad,
                        Integer idDocenteDecano, String nombreDecano) {
        this.idFacultad = idFacultad;
        this.nombre = nombre;
        this.idUniversidad = idUniversidad;
        this.nombreUniversidad = nombreUniversidad;
        this.idDocenteDecano = idDocenteDecano;
        this.nombreDecano = nombreDecano;
    }

    public Integer getIdFacultad() { return idFacultad; }
    public void setIdFacultad(Integer idFacultad) { this.idFacultad = idFacultad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getIdUniversidad() { return idUniversidad; }
    public void setIdUniversidad(Integer idUniversidad) { this.idUniversidad = idUniversidad; }
    public String getNombreUniversidad() { return nombreUniversidad; }
    public void setNombreUniversidad(String nombreUniversidad) { this.nombreUniversidad = nombreUniversidad; }
    public Integer getIdDocenteDecano() { return idDocenteDecano; }
    public void setIdDocenteDecano(Integer idDocenteDecano) { this.idDocenteDecano = idDocenteDecano; }
    public String getNombreDecano() { return nombreDecano; }
    public void setNombreDecano(String nombreDecano) { this.nombreDecano = nombreDecano; }
}
