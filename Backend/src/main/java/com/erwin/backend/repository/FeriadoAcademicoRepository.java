package com.erwin.backend.repository;

import com.erwin.backend.entities.FeriadoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface FeriadoAcademicoRepository extends JpaRepository<FeriadoAcademico, Long> {

    List<FeriadoAcademico> findAllByOrderByFechaAsc();

    default Set<LocalDate> fechasComoSet() {
        return findAll().stream().map(FeriadoAcademico::getFecha).collect(Collectors.toSet());
    }
}
