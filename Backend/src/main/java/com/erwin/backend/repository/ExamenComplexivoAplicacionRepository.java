package com.erwin.backend.repository;

import com.erwin.backend.entities.ExamenComplexivoAplicacion;
import com.erwin.backend.enums.EstadoExamenComplexivo;
import com.erwin.backend.enums.TipoExamenComplexivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamenComplexivoAplicacionRepository extends JpaRepository<ExamenComplexivoAplicacion, Long> {

    List<ExamenComplexivoAplicacion> findByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(
            Integer idEstudiante, Integer idPeriodo);

    Optional<ExamenComplexivoAplicacion> findFirstByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodoAndTipoOrderByIdDesc(
            Integer idEstudiante, Integer idPeriodo, TipoExamenComplexivo tipo);

    List<ExamenComplexivoAplicacion> findByEstadoAndHoraFinBefore(
            EstadoExamenComplexivo estado, LocalDateTime instante);

    List<ExamenComplexivoAplicacion> findByPeriodoTitulacion_IdPeriodo(Integer idPeriodo);

    /** Fila: [idCarrera, nombreCarrera, total, aprobados, reprobados, promedioCalificacion] — solo FINALIZADO. */
    @Query("""
        SELECT c.idCarrera, c.nombre,
               COUNT(e),
               SUM(CASE WHEN e.calificacion >= :notaAprobacion THEN 1L ELSE 0L END),
               SUM(CASE WHEN e.calificacion < :notaAprobacion THEN 1L ELSE 0L END),
               AVG(e.calificacion)
        FROM ExamenComplexivoAplicacion e
        JOIN e.estudiante est
        JOIN est.carrera c
        WHERE c.facultad.idFacultad = :idFacultad
          AND e.periodoTitulacion.descripcion = :periodo
          AND e.estado = com.erwin.backend.enums.EstadoExamenComplexivo.FINALIZADO
        GROUP BY c.idCarrera, c.nombre
        """)
    List<Object[]> resultadosPorCarrera(@Param("idFacultad") Integer idFacultad, @Param("periodo") String periodo,
                                         @Param("notaAprobacion") java.math.BigDecimal notaAprobacion);

    /** Fila: [total, aprobados, reprobados] a nivel institucional para el dashboard — solo FINALIZADO. */
    @Query("""
        SELECT COUNT(e),
               SUM(CASE WHEN e.calificacion >= :notaAprobacion THEN 1L ELSE 0L END),
               SUM(CASE WHEN e.calificacion < :notaAprobacion THEN 1L ELSE 0L END)
        FROM ExamenComplexivoAplicacion e
        WHERE e.periodoTitulacion.descripcion = :periodo
          AND e.estado = com.erwin.backend.enums.EstadoExamenComplexivo.FINALIZADO
        """)
    List<Object[]> resumenGlobal(@Param("periodo") String periodo, @Param("notaAprobacion") java.math.BigDecimal notaAprobacion);

    /** Fecha/hora del examen complexivo FINALIZADO más reciente de la facultad — ancla del plazo del Art. 40. */
    @Query("""
        SELECT MAX(e.horaFin)
        FROM ExamenComplexivoAplicacion e
        JOIN e.estudiante est
        JOIN est.carrera c
        WHERE c.facultad.idFacultad = :idFacultad
          AND e.periodoTitulacion.descripcion = :periodo
          AND e.estado = com.erwin.backend.enums.EstadoExamenComplexivo.FINALIZADO
        """)
    LocalDateTime ultimaFechaExamenFinalizado(@Param("idFacultad") Integer idFacultad, @Param("periodo") String periodo);
}
