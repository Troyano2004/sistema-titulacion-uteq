package com.erwin.backend.repository;

import com.erwin.backend.entities.ReferenciaBibliografica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenciaBibliograficaRepository extends JpaRepository<ReferenciaBibliografica, Long> {
    List<ReferenciaBibliografica> findByDocumento_Id(Long idDocumento);
}
