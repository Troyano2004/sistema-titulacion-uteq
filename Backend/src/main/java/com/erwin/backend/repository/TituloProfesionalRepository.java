package com.erwin.backend.repository;

import com.erwin.backend.entities.TituloProfesional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TituloProfesionalRepository extends JpaRepository<TituloProfesional, Long> {
    Optional<TituloProfesional> findByActaGrado_IdActa(Integer idActa);
    Optional<TituloProfesional> findByEstudiante_IdEstudiante(Integer idEstudiante);
}
