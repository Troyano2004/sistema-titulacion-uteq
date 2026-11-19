package com.erwin.backend.repository;

import com.erwin.backend.entities.ExamenComplexivoRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamenComplexivoRespuestaRepository extends JpaRepository<ExamenComplexivoRespuesta, Long> {

    List<ExamenComplexivoRespuesta> findByExamen_IdOrderByOrden(Long idExamen);

    Optional<ExamenComplexivoRespuesta> findByExamen_IdAndPregunta_Id(Long idExamen, Long idPregunta);

    long countByExamen_IdAndOpcionSeleccionada_EsCorrectaTrue(Long idExamen);
}
