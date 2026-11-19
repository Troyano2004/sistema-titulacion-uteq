package com.erwin.backend.repository;

import com.erwin.backend.entities.LineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineaInvestigacionRepository extends JpaRepository<LineaInvestigacion, Long> {
    List<LineaInvestigacion> findByCarrera_IdCarrera(Integer idCarrera);
}
