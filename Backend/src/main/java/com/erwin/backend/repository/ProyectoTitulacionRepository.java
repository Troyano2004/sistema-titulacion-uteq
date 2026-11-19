

package com.erwin.backend.repository;
import com.erwin.backend.entities.ProyectoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ProyectoTitulacionRepository extends JpaRepository<ProyectoTitulacion, Integer> {
    Optional<ProyectoTitulacion> findByPropuesta_IdPropuesta(Integer idPropuesta);
    List<ProyectoTitulacion> findByDirector_IdDocente(Integer IdDocente);
    long countByEstado(String estado);
    long countByDirector_IdDocente(Integer idDocente);
    Optional<ProyectoTitulacion> findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(Integer idEstudiante);

    /** Fila: [idCarrera, nombreCarrera, nombreModalidad(TIC), total, aprobados, reprobados, promedioNota] */
    @Query("""
        SELECT c.idCarrera, c.nombre, tt.nombre,
               COUNT(p),
               SUM(CASE WHEN p.estado = 'FINALIZADO' THEN 1L ELSE 0L END),
               SUM(CASE WHEN p.estado = 'REPROBADO' THEN 1L ELSE 0L END),
               AVG(p.notaPredefensaFinal)
        FROM ProyectoTitulacion p
        JOIN p.eleccion e
        JOIN e.carrera c
        JOIN p.tipoTrabajo tt
        WHERE c.facultad.idFacultad = :idFacultad
          AND p.periodo.descripcion = :periodo
        GROUP BY c.idCarrera, c.nombre, tt.nombre
        """)
    List<Object[]> resultadosPorCarreraYModalidad(@Param("idFacultad") Integer idFacultad, @Param("periodo") String periodo);

    /** Fila: [nombreModalidad(TIC), total, aprobados, reprobados, enProceso] para el dashboard general. */
    @Query("""
        SELECT tt.nombre,
               COUNT(p),
               SUM(CASE WHEN p.estado = 'FINALIZADO' THEN 1L ELSE 0L END),
               SUM(CASE WHEN p.estado = 'REPROBADO' THEN 1L ELSE 0L END),
               SUM(CASE WHEN p.estado NOT IN ('FINALIZADO','REPROBADO') THEN 1L ELSE 0L END)
        FROM ProyectoTitulacion p
        JOIN p.tipoTrabajo tt
        WHERE p.periodo.descripcion = :periodo
        GROUP BY tt.nombre
        """)
    List<Object[]> resumenPorModalidad(@Param("periodo") String periodo);
}