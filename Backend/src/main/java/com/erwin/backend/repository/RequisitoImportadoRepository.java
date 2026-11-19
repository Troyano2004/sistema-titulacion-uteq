package com.erwin.backend.repository;

import com.erwin.backend.entities.RequisitoImportado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequisitoImportadoRepository extends JpaRepository<RequisitoImportado, Long> {
    Optional<RequisitoImportado> findByEstudiante_IdEstudiante(Integer idEstudiante);
}
