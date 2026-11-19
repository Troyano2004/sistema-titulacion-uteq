package com.erwin.backend.repository;

import com.erwin.backend.entities.MatriculaTitulacion;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatriculaTitulacionRepository extends JpaRepository<MatriculaTitulacion, Long> {
    List<MatriculaTitulacion> findByEstudiante_IdEstudiante(Integer idEstudiante);

    Optional<MatriculaTitulacion> findByEstudiante_IdEstudianteAndNivelAndEstado(
            Integer idEstudiante, NivelMatricula nivel, EstadoMatricula estado);

    boolean existsByEstudiante_IdEstudianteAndNivelAndEstadoIn(
            Integer idEstudiante, NivelMatricula nivel, Collection<EstadoMatricula> estados);

    List<MatriculaTitulacion> findByPeriodoAcademicoAndNivelAndEstado(
            String periodoAcademico, NivelMatricula nivel, EstadoMatricula estado);

    /** Fila: [nivel, cantidad] — para "total matriculados por modalidad" del dashboard. */
    @Query("SELECT m.nivel, COUNT(m) FROM MatriculaTitulacion m WHERE m.periodoAcademico = :periodo GROUP BY m.nivel")
    List<Object[]> conteoPorNivel(@Param("periodo") String periodo);

    long countByPeriodoAcademico(String periodoAcademico);

    /**
     * Fecha de primera matrícula (Titulación I) y fecha de emisión del acta de grado,
     * por estudiante, para calcular el tiempo promedio de titulación — proyección
     * mínima de dos columnas, no carga de entidades completas.
     */
    @Query("""
        SELECT m.fechaMatricula, a.fechaEmision
        FROM MatriculaTitulacion m, com.erwin.backend.entities.ActaGrado a
        WHERE m.estudiante.idEstudiante = a.estudiante.idEstudiante
          AND m.nivel = com.erwin.backend.enums.NivelMatricula.TITULACION_I
        """)
    List<Object[]> fechasMatriculaYGradoParaTiempoPromedio();
}
