package com.erwin.backend.service;

import com.erwin.backend.entities.SeccionPlantillaTic;
import com.erwin.backend.enums.TipoTicPlantilla;
import com.erwin.backend.repository.SeccionPlantillaTicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Estructura de secciones por tipo de TIC (Art. 42/43/44/45 + preliminares
 * del Art. 59), ahora leída de la BD (tabla seccion_plantilla_tic, ver
 * migración V18) en vez de un switch hardcodeado.
 *
 * El Art. 59 exige que TODA modalidad de TIC lleve, después de la Portada,
 * Certificación del Director + Autoría + Dedicatoria + Agradecimiento — los
 * Arts. 44/45 no las repiten en su propia lista (por eso el código viejo no
 * las traía para Proyecto Tecnológico / Estudio de Caso), y el Art. 43 no
 * define estructura propia para Artículo Científico (por eso le faltaban
 * hasta las secciones que comparten las 4 modalidades). Estos tests
 * verifican que las 4 plantillas ya las incluyen.
 */
@ExtendWith(MockitoExtension.class)
class SeccionPlantillaTicServiceTest {

    @Mock private SeccionPlantillaTicRepository repo;
    private SeccionPlantillaTicService service;

    @BeforeEach
    void setUp() {
        service = new SeccionPlantillaTicService(repo);
    }

    private SeccionPlantillaTic seccion(String tipo, String codigo, String titulo, int orden,
                                         boolean obligatoria, boolean autoLlenada) {
        SeccionPlantillaTic s = new SeccionPlantillaTic();
        s.setTipoPlantilla(tipo);
        s.setCodigoSeccion(codigo);
        s.setTitulo(titulo);
        s.setOrden(orden);
        s.setObligatoria(obligatoria);
        s.setAutoLlenada(autoLlenada);
        return s;
    }

    @Test
    @DisplayName("PROYECTO_INVESTIGACION (Art. 42): trae Dedicatoria y Agradecimiento")
    void proyectoInvestigacionTraeDedicatoriaYAgradecimiento() {
        when(repo.findByTipoPlantillaOrderByOrden("PROYECTO_INVESTIGACION")).thenReturn(List.of(
                seccion("PROYECTO_INVESTIGACION", "PORTADA", "Portada", 1, true, true),
                seccion("PROYECTO_INVESTIGACION", "CERTIFICACION_DIRECTOR", "Certificación del Director", 2, true, true),
                seccion("PROYECTO_INVESTIGACION", "AUTORIA", "Autoría", 3, true, false),
                seccion("PROYECTO_INVESTIGACION", "DEDICATORIA", "Dedicatoria", 4, false, false),
                seccion("PROYECTO_INVESTIGACION", "AGRADECIMIENTO", "Agradecimiento", 5, false, false)
        ));

        List<SeccionPlantillaProvider.SeccionDef> secciones = service.plantillaPara(TipoTicPlantilla.PROYECTO_INVESTIGACION);

        assertThat(secciones).extracting(SeccionPlantillaProvider.SeccionDef::codigo)
                .contains("DEDICATORIA", "AGRADECIMIENTO");
    }

    @Test
    @DisplayName("Art. 59: PROYECTO_TECNOLOGICO también lleva Certificación, Autoría, Dedicatoria y Agradecimiento (el Art. 45 no las repite, pero el Art. 59 las exige)")
    void proyectoTecnologicoIncluyePreliminaresDelArt59() {
        when(repo.findByTipoPlantillaOrderByOrden("PROYECTO_TECNOLOGICO")).thenReturn(List.of(
                seccion("PROYECTO_TECNOLOGICO", "PORTADA", "Portada", 1, true, true),
                seccion("PROYECTO_TECNOLOGICO", "CERTIFICACION_DIRECTOR", "Certificación del Director", 2, true, true),
                seccion("PROYECTO_TECNOLOGICO", "AUTORIA", "Autoría", 3, true, false),
                seccion("PROYECTO_TECNOLOGICO", "DEDICATORIA", "Dedicatoria", 4, false, false),
                seccion("PROYECTO_TECNOLOGICO", "AGRADECIMIENTO", "Agradecimiento", 5, false, false),
                seccion("PROYECTO_TECNOLOGICO", "MARCO_CONTEXTUAL", "Marco Contextual del Proyecto Tecnológico", 11, true, false),
                seccion("PROYECTO_TECNOLOGICO", "METODOLOGIA_PT", "Metodología", 14, true, false),
                seccion("PROYECTO_TECNOLOGICO", "RESULTADOS_PT", "Resultados", 15, true, false)
        ));

        List<SeccionPlantillaProvider.SeccionDef> secciones = service.plantillaPara(TipoTicPlantilla.PROYECTO_TECNOLOGICO);

        assertThat(secciones).extracting(SeccionPlantillaProvider.SeccionDef::codigo)
                .contains("CERTIFICACION_DIRECTOR", "AUTORIA", "DEDICATORIA", "AGRADECIMIENTO",
                        "MARCO_CONTEXTUAL", "METODOLOGIA_PT", "RESULTADOS_PT");
    }

    @Test
    @DisplayName("Art. 59: ESTUDIO_DE_CASO también lleva Dedicatoria y Agradecimiento")
    void estudioDeCasoIncluyePreliminaresDelArt59() {
        when(repo.findByTipoPlantillaOrderByOrden("ESTUDIO_DE_CASO")).thenReturn(List.of(
                seccion("ESTUDIO_DE_CASO", "PORTADA", "Portada", 1, true, true),
                seccion("ESTUDIO_DE_CASO", "DEDICATORIA", "Dedicatoria", 4, false, false),
                seccion("ESTUDIO_DE_CASO", "AGRADECIMIENTO", "Agradecimiento", 5, false, false),
                seccion("ESTUDIO_DE_CASO", "DESCRIPCION_CASO", "Descripción del Caso en Estudio", 11, true, false),
                seccion("ESTUDIO_DE_CASO", "CONCLUSIONES_PLAN_EDUCATIVO", "Conclusiones o Plan Educativo", 15, true, false)
        ));

        List<SeccionPlantillaProvider.SeccionDef> secciones = service.plantillaPara(TipoTicPlantilla.ESTUDIO_DE_CASO);

        assertThat(secciones).extracting(SeccionPlantillaProvider.SeccionDef::codigo)
                .contains("DEDICATORIA", "AGRADECIMIENTO", "DESCRIPCION_CASO", "CONCLUSIONES_PLAN_EDUCATIVO");
    }

    @Test
    @DisplayName("Art. 59 + comunes a las 4 modalidades: ARTICULO_CIENTIFICO ya no queda sin Certificación/Autoría/Índice/Código Dublín/Anexos")
    void articuloCientificoIncluyePreliminaresYComunes() {
        when(repo.findByTipoPlantillaOrderByOrden("ARTICULO_CIENTIFICO")).thenReturn(List.of(
                seccion("ARTICULO_CIENTIFICO", "PORTADA", "Portada", 1, true, true),
                seccion("ARTICULO_CIENTIFICO", "CERTIFICACION_DIRECTOR", "Certificación del Director", 2, true, true),
                seccion("ARTICULO_CIENTIFICO", "AUTORIA", "Autoría", 3, true, false),
                seccion("ARTICULO_CIENTIFICO", "DEDICATORIA", "Dedicatoria", 4, false, false),
                seccion("ARTICULO_CIENTIFICO", "AGRADECIMIENTO", "Agradecimiento", 5, false, false),
                seccion("ARTICULO_CIENTIFICO", "INDICE", "Índice", 8, true, true),
                seccion("ARTICULO_CIENTIFICO", "CODIGO_DUBLIN", "Código Dublín", 9, true, true),
                seccion("ARTICULO_CIENTIFICO", "METODOLOGIA", "Metodología", 11, true, false),
                seccion("ARTICULO_CIENTIFICO", "RESULTADOS", "Resultados", 12, true, false),
                seccion("ARTICULO_CIENTIFICO", "ANEXOS", "Anexos", 16, false, false)
        ));

        List<SeccionPlantillaProvider.SeccionDef> secciones = service.plantillaPara(TipoTicPlantilla.ARTICULO_CIENTIFICO);

        assertThat(secciones).extracting(SeccionPlantillaProvider.SeccionDef::codigo)
                .contains("CERTIFICACION_DIRECTOR", "AUTORIA", "DEDICATORIA", "AGRADECIMIENTO",
                        "INDICE", "CODIGO_DUBLIN", "ANEXOS");
    }

    @Test
    @DisplayName("el orden de la BD se respeta tal cual (ORDER BY orden)")
    void respetaElOrdenDeLaBd() {
        when(repo.findByTipoPlantillaOrderByOrden("PROYECTO_INVESTIGACION")).thenReturn(List.of(
                seccion("PROYECTO_INVESTIGACION", "PORTADA", "Portada", 1, true, true),
                seccion("PROYECTO_INVESTIGACION", "CERTIFICACION_DIRECTOR", "Certificación del Director", 2, true, true),
                seccion("PROYECTO_INVESTIGACION", "AUTORIA", "Autoría", 3, true, false)
        ));

        List<SeccionPlantillaProvider.SeccionDef> secciones = service.plantillaPara(TipoTicPlantilla.PROYECTO_INVESTIGACION);

        assertThat(secciones).extracting(SeccionPlantillaProvider.SeccionDef::codigo)
                .containsExactly("PORTADA", "CERTIFICACION_DIRECTOR", "AUTORIA");
    }
}
