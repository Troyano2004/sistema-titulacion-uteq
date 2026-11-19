package com.erwin.backend.repository;

import com.erwin.backend.entities.CatalogoDocumentoHabilitante;
import com.erwin.backend.enums.ModalidadCatalogoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogoDocumentoHabilitanteRepository extends JpaRepository<CatalogoDocumentoHabilitante, Long> {
    List<CatalogoDocumentoHabilitante> findByModalidadInOrderByOrdenAsc(List<ModalidadCatalogoDocumento> modalidades);
}
