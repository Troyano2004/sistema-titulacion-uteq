package com.erwin.backend.repository;

import com.erwin.backend.entities.VersionDocumentoTIC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionDocumentoTicRepository extends JpaRepository<VersionDocumentoTIC, Long> {
    List<VersionDocumentoTIC> findByDocumento_IdOrderByNumeroVersionDesc(Long idDocumento);
    Optional<VersionDocumentoTIC> findFirstByDocumento_IdOrderByNumeroVersionDesc(Long idDocumento);
}
