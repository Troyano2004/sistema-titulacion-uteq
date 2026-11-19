package com.erwin.backend.repository;

import com.erwin.backend.entities.ExpedienteEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpedienteEstudianteRepository extends JpaRepository<ExpedienteEstudiante, Long> {
    Optional<ExpedienteEstudiante> findFirstByEstudiante_IdEstudianteOrderByIdDesc(Integer idEstudiante);
    List<ExpedienteEstudiante> findByEstudiante_Carrera_IdCarrera(Integer idCarrera);

    /** Fila: [estado, cantidad] para el reporte de expedientes, filtrable por carrera/periodo. */
    @Query("""
        SELECT exp.estado, COUNT(exp)
        FROM ExpedienteEstudiante exp
        WHERE (:idCarrera IS NULL OR exp.estudiante.carrera.idCarrera = :idCarrera)
          AND (:periodo IS NULL OR exp.periodoTitulacion = :periodo)
        GROUP BY exp.estado
        """)
    List<Object[]> resumenPorEstado(@Param("idCarrera") Integer idCarrera, @Param("periodo") String periodo);
}
