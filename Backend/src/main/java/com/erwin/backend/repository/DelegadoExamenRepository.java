package com.erwin.backend.repository;

import com.erwin.backend.entities.DelegadoExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelegadoExamenRepository extends JpaRepository<DelegadoExamen, Long> {
    List<DelegadoExamen> findByExamen_Id(Long idExamen);
}
