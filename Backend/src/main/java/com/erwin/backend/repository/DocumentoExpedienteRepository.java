package com.erwin.backend.repository;

import com.erwin.backend.entities.DocumentoExpediente;
import com.erwin.backend.enums.EstadoDocumentoExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentoExpedienteRepository extends JpaRepository<DocumentoExpediente, Long> {
    List<DocumentoExpediente> findByExpediente_IdOrderByCatalogoDocumento_OrdenAsc(Long idExpediente);
    List<DocumentoExpediente> findByEstado(EstadoDocumentoExpediente estado);
    long countByExpediente_Id(Long idExpediente);
    long countByExpediente_IdAndEstado(Long idExpediente, EstadoDocumentoExpediente estado);

    /**
     * Fila: [nombreDocumento, vecesFaltante] — documentos PENDIENTE o RECHAZADO
     * (es decir, aún no entregados/aprobados), agrupados por catálogo, para
     * encontrar los documentos habilitantes más frecuentemente faltantes.
     */
    @Query("""
        SELECT cat.nombre, COUNT(d)
        FROM DocumentoExpediente d
        JOIN d.catalogoDocumento cat
        WHERE d.estado IN (com.erwin.backend.enums.EstadoDocumentoExpediente.PENDIENTE,
                            com.erwin.backend.enums.EstadoDocumentoExpediente.RECHAZADO)
          AND (:idCarrera IS NULL OR d.expediente.estudiante.carrera.idCarrera = :idCarrera)
          AND (:periodo IS NULL OR d.expediente.periodoTitulacion = :periodo)
        GROUP BY cat.nombre
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> documentosFaltantesFrecuentes(@Param("idCarrera") Integer idCarrera, @Param("periodo") String periodo);
}
