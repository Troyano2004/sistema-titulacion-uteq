package com.erwin.backend.service;

import com.erwin.backend.enums.TipoTicPlantilla;
import com.erwin.backend.repository.SeccionPlantillaTicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Estructura de secciones por tipo de TIC (Art. 42/43/44/45 + preliminares
 * del Art. 59), leída de la tabla seccion_plantilla_tic en vez de un switch
 * hardcodeado (ver migración V18 y SeccionPlantillaProvider.clasificar,
 * que sigue siendo quien decide QUÉ tipo de plantilla aplica).
 */
@Service
@Transactional(readOnly = true)
public class SeccionPlantillaTicService {

    private final SeccionPlantillaTicRepository repo;

    public SeccionPlantillaTicService(SeccionPlantillaTicRepository repo) {
        this.repo = repo;
    }

    public List<SeccionPlantillaProvider.SeccionDef> plantillaPara(TipoTicPlantilla tipo) {
        return repo.findByTipoPlantillaOrderByOrden(tipo.name()).stream()
                .map(s -> new SeccionPlantillaProvider.SeccionDef(
                        s.getCodigoSeccion(), s.getTitulo(), s.getObligatoria(), s.getAutoLlenada()))
                .toList();
    }
}
