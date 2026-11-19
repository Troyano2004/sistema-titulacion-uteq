package com.erwin.backend.service;

import com.erwin.backend.enums.TipoTicPlantilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TAREA 1.2: clasificación del tipo de trabajo hacia una de las 4 plantillas
 * de TIC. La ESTRUCTURA de cada plantilla ya no vive aquí — se movió a la
 * tabla seccion_plantilla_tic (migración V18); ver SeccionPlantillaTicServiceTest.
 */
class SeccionPlantillaProviderTest {

    @Test
    @DisplayName("clasificar() reconoce Proyecto Tecnológico, Estudio de Caso y Artículo Científico por palabras clave")
    void clasificarReconoceLosCuatroTipos() {
        assertThat(SeccionPlantillaProvider.clasificar("Proyecto de Investigación")).isEqualTo(TipoTicPlantilla.PROYECTO_INVESTIGACION);
        assertThat(SeccionPlantillaProvider.clasificar("Proyecto Tecnológico")).isEqualTo(TipoTicPlantilla.PROYECTO_TECNOLOGICO);
        assertThat(SeccionPlantillaProvider.clasificar("Estudio de Caso")).isEqualTo(TipoTicPlantilla.ESTUDIO_DE_CASO);
        assertThat(SeccionPlantillaProvider.clasificar("Artículo Científico")).isEqualTo(TipoTicPlantilla.ARTICULO_CIENTIFICO);
    }

    @Test
    @DisplayName("clasificar() usa PROYECTO_INVESTIGACION como respaldo si el nombre no coincide con ninguna palabra clave")
    void clasificarUsaRespaldoSiNoReconoce() {
        assertThat(SeccionPlantillaProvider.clasificar("Modalidad Desconocida")).isEqualTo(TipoTicPlantilla.PROYECTO_INVESTIGACION);
        assertThat(SeccionPlantillaProvider.clasificar(null)).isEqualTo(TipoTicPlantilla.PROYECTO_INVESTIGACION);
    }
}
