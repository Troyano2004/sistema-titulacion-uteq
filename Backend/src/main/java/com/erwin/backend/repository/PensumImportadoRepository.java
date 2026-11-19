package com.erwin.backend.repository;

import com.erwin.backend.entities.PensumImportado;
import com.erwin.backend.enums.EstadoAsignaturaImportada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PensumImportadoRepository extends JpaRepository<PensumImportado, Long> {
    List<PensumImportado> findByEstudiante_IdEstudiante(Integer idEstudiante);
    List<PensumImportado> findByEstudiante_IdEstudianteAndEstado(Integer idEstudiante, EstadoAsignaturaImportada estado);
    void deleteByEstudiante_IdEstudianteAndCodigoAsignaturaAndPeriodoAcademico(
            Integer idEstudiante, String codigoAsignatura, String periodoAcademico);
}
