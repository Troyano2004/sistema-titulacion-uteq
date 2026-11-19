package com.erwin.backend.repository;

import com.erwin.backend.entities.EjeTematico;
import com.erwin.backend.enums.EstadoEjeTematico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EjeTematicoRepository extends JpaRepository<EjeTematico, Long> {
    List<EjeTematico> findByCarrera_IdCarrera(Integer idCarrera);

    List<EjeTematico> findByCarrera_IdCarreraAndPeriodoAcademico(Integer idCarrera, String periodoAcademico);

    long countByCarrera_IdCarreraAndPeriodoAcademicoAndEstado(
            Integer idCarrera, String periodoAcademico, EstadoEjeTematico estado);

    List<EjeTematico> findByCarrera_IdCarreraAndDocenteResponsableIsNotNull(Integer idCarrera);
}
