package com.erwin.backend.repository;

import com.erwin.backend.entities.SeccionPlantillaTic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeccionPlantillaTicRepository extends JpaRepository<SeccionPlantillaTic, Integer> {
    List<SeccionPlantillaTic> findByTipoPlantillaOrderByOrden(String tipoPlantilla);
}
