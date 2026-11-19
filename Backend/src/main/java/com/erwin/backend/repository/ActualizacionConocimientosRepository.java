package com.erwin.backend.repository;

import com.erwin.backend.entities.ActualizacionConocimientos;
import com.erwin.backend.enums.EstadoActualizacionConocimientos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActualizacionConocimientosRepository extends JpaRepository<ActualizacionConocimientos, Long> {

    List<ActualizacionConocimientos> findByEstudiante_IdEstudianteOrderByFechaSolicitudDesc(Integer idEstudiante);

    Optional<ActualizacionConocimientos> findFirstByEstudiante_IdEstudianteAndEstadoOrderByFechaSolicitudDesc(
            Integer idEstudiante, EstadoActualizacionConocimientos estado);
}
