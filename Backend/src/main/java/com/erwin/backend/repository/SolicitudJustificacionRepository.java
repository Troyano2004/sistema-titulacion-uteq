package com.erwin.backend.repository;

import com.erwin.backend.entities.SolicitudJustificacion;
import com.erwin.backend.enums.EstadoJustificacion;
import com.erwin.backend.enums.TipoJustificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitudJustificacionRepository extends JpaRepository<SolicitudJustificacion, Long> {

    List<SolicitudJustificacion> findByEstudiante_IdEstudianteOrderByFechaSolicitudDesc(Integer idEstudiante);

    List<SolicitudJustificacion> findByEstadoOrderByFechaSolicitudAsc(EstadoJustificacion estado);

    boolean existsByEstudiante_IdEstudianteAndTipo(Integer idEstudiante, TipoJustificacion tipo);

    List<SolicitudJustificacion> findByEstadoAndFechaLimiteBefore(EstadoJustificacion estado, LocalDateTime instante);
}
