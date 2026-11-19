package com.erwin.backend.repository;

import com.erwin.backend.entities.ImportacionSgaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportacionSgaLogRepository extends JpaRepository<ImportacionSgaLog, Long> {
    List<ImportacionSgaLog> findAllByOrderByFechaImportacionDesc();
}
