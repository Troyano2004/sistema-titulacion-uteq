package com.erwin.backend.repository;

import com.erwin.backend.entities.AutorizacionTerceraMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorizacionTerceraMatriculaRepository extends JpaRepository<AutorizacionTerceraMatricula, Long> {
    boolean existsByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(Integer idEstudiante, Integer idPeriodo);
}
