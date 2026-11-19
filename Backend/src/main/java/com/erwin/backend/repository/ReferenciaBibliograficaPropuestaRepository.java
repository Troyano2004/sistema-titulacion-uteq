package com.erwin.backend.repository;

import com.erwin.backend.entities.ReferenciaBibliograficaPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenciaBibliograficaPropuestaRepository extends JpaRepository<ReferenciaBibliograficaPropuesta, Integer> {
    List<ReferenciaBibliograficaPropuesta> findByPropuesta_IdPropuesta(Integer idPropuesta);
}
