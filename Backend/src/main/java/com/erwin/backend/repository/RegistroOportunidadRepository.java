package com.erwin.backend.repository;

import com.erwin.backend.entities.RegistroOportunidad;
import com.erwin.backend.enums.ModalidadOportunidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroOportunidadRepository extends JpaRepository<RegistroOportunidad, Long> {

    List<RegistroOportunidad> findByEstudiante_IdEstudianteOrderByFechaAsc(Integer idEstudiante);

    List<RegistroOportunidad> findByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(
            Integer idEstudiante, Integer idPeriodo);

    List<RegistroOportunidad> findByEstudiante_IdEstudianteAndModalidadOrderByFechaAsc(
            Integer idEstudiante, ModalidadOportunidad modalidad);
}
