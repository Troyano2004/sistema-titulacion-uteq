package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoTIC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoTicRepository extends JpaRepository<DocumentoTIC, Long> {
    List<DocumentoTIC> findByEstudiante_IdEstudiante(Integer idEstudiante);
    Optional<DocumentoTIC> findFirstByEstudiante_IdEstudianteOrderByIdDesc(Integer idEstudiante);
    List<DocumentoTIC> findByDirector_IdDocente(Integer idDocente);
}
