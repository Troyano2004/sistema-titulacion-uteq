package com.erwin.backend.repository;

import com.erwin.backend.entities.PreguntaComplexivo;
import com.erwin.backend.enums.EstadoPregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaComplexivoRepository extends JpaRepository<PreguntaComplexivo, Long> {

    List<PreguntaComplexivo> findByCarrera_IdCarrera(Integer idCarrera);

    List<PreguntaComplexivo> findByCarrera_IdCarreraAndEstado(Integer idCarrera, EstadoPregunta estado);

    List<PreguntaComplexivo> findByCarrera_IdCarreraAndEjeTematico_Id(Integer idCarrera, Long idEjeTematico);

    List<PreguntaComplexivo> findByCarrera_IdCarreraAndEjeTematico_IdAndEstado(
            Integer idCarrera, Long idEjeTematico, EstadoPregunta estado);

    List<PreguntaComplexivo> findByEjeTematico_IdAndEstado(Long idEjeTematico, EstadoPregunta estado);

    long countByCarrera_IdCarreraAndEstado(Integer idCarrera, EstadoPregunta estado);

    long countByCarrera_IdCarreraAndEjeTematico_IdAndEstado(
            Integer idCarrera, Long idEjeTematico, EstadoPregunta estado);
}
