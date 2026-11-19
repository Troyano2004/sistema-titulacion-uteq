package com.erwin.backend.repository;

import com.erwin.backend.entities.ComentarioDocumentoTIC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioDocumentoTicRepository extends JpaRepository<ComentarioDocumentoTIC, Long> {
    List<ComentarioDocumentoTIC> findByDocumento_IdOrderByFechaCreacionAsc(Long idDocumento);
    List<ComentarioDocumentoTIC> findByDocumento_IdAndSeccion_IdOrderByFechaCreacionAsc(Long idDocumento, Long idSeccion);
}
