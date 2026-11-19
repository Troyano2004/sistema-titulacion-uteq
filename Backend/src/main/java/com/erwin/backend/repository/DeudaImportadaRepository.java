package com.erwin.backend.repository;

import com.erwin.backend.entities.DeudaImportada;
import com.erwin.backend.enums.EstadoDeudaImportada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeudaImportadaRepository extends JpaRepository<DeudaImportada, Long> {
    List<DeudaImportada> findByEstudiante_IdEstudiante(Integer idEstudiante);
    boolean existsByEstudiante_IdEstudianteAndEstado(Integer idEstudiante, EstadoDeudaImportada estado);
}
