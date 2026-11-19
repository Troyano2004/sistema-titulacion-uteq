package com.erwin.backend.repository;

import com.erwin.backend.entities.ActaGrado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface ActaGradoRepository extends JpaRepository<ActaGrado, Integer> {

    Optional<ActaGrado> findByEstudiante_IdEstudiante(Integer idEstudiante);

    boolean existsByEstudiante_IdEstudiante(Integer idEstudiante);

    @Query("SELECT AVG(a.notaFinalGrado) FROM ActaGrado a WHERE a.notaFinalGrado IS NOT NULL")
    BigDecimal promedioNotaFinalGradoGlobal();
}
