package com.erwin.backend.repository;

import com.erwin.backend.entities.SeccionDocumentoTIC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeccionDocumentoTicRepository extends JpaRepository<SeccionDocumentoTIC, Long> {
    List<SeccionDocumentoTIC> findByDocumento_IdOrderByOrdenAsc(Long idDocumento);
    Optional<SeccionDocumentoTIC> findByDocumento_IdAndCodigoSeccion(Long idDocumento, String codigoSeccion);
    long countByDocumento_Id(Long idDocumento);
    long countByDocumento_IdAndEstado(Long idDocumento, com.erwin.backend.enums.EstadoSeccionTic estado);
}
