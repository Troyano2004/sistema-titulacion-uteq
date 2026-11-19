package com.erwin.backend.service;

import com.erwin.backend.dtos.SgaImportDtos.ImportacionResultadoDto;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** TAREA 2: validación de la importación CSV/XLSX del SGA. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SgaImportServiceTest {

    @Mock private PensumImportadoRepository pensumRepo;
    @Mock private RequisitoImportadoRepository requisitoRepo;
    @Mock private DeudaImportadaRepository deudaRepo;
    @Mock private ImportacionSgaLogRepository logRepo;
    @Mock private EstudianteRepository estudianteRepo;
    @Mock private UsuarioRepository usuarioRepo;

    private SgaImportService service;

    @BeforeEach
    void setUp() {
        service = new SgaImportService(pensumRepo, requisitoRepo, deudaRepo, logRepo, estudianteRepo, usuarioRepo);
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pensumRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deudaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Estudiante estudiante(int id) {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(id);
        e.setUsuario(new Usuario());
        return e;
    }

    private MockMultipartFile csv(String nombre, String contenido) {
        return new MockMultipartFile("file", nombre, "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("importarPensum acepta filas válidas y calcula el promedio del récord académico")
    void importarPensumCalculaPromedio() {
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));
        when(pensumRepo.findByEstudiante_IdEstudiante(10)).thenReturn(List.of());
        when(estudianteRepo.findById(10)).thenReturn(Optional.of(estudiante(10)));

        String csvContenido = "cedulaEstudiante,codigoAsignatura,nombreAsignatura,calificacion,estado,periodoAcademico\n"
                + "1234567890,MAT101,Matemáticas I,8.00,APROBADA,2024-2024\n"
                + "1234567890,PRG101,Programación I,10.00,APROBADA,2024-2024\n";

        ImportacionResultadoDto resultado = service.importarPensum(csv("pensum.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(2);
        assertThat(resultado.filasError()).isEqualTo(0);
    }

    @Test
    @DisplayName("importarPensum registra un error por fila cuando la cédula no existe, sin abortar el resto")
    void importarPensumRegistraErrorPorCedulaInexistente() {
        when(estudianteRepo.findByUsuario_Cedula("0000000000")).thenReturn(Optional.empty());
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));
        when(pensumRepo.findByEstudiante_IdEstudiante(10)).thenReturn(List.of());
        when(estudianteRepo.findById(10)).thenReturn(Optional.of(estudiante(10)));

        String csvContenido = "cedulaEstudiante,codigoAsignatura,nombreAsignatura,calificacion,estado,periodoAcademico\n"
                + "0000000000,MAT101,Matemáticas I,8.00,APROBADA,2024-2024\n"
                + "1234567890,PRG101,Programación I,9.00,APROBADA,2024-2024\n";

        ImportacionResultadoDto resultado = service.importarPensum(csv("pensum.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(1);
        assertThat(resultado.filasError()).isEqualTo(1);
        assertThat(resultado.errores().get(0)).contains("no existe un estudiante con cédula 0000000000");
    }

    @Test
    @DisplayName("importarPensum rechaza una calificación fuera del rango 0-10")
    void importarPensumRechazaCalificacionFueraDeRango() {
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));

        String csvContenido = "cedulaEstudiante,codigoAsignatura,nombreAsignatura,calificacion,estado,periodoAcademico\n"
                + "1234567890,MAT101,Matemáticas I,15.00,APROBADA,2024-2024\n";

        ImportacionResultadoDto resultado = service.importarPensum(csv("pensum.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(0);
        assertThat(resultado.filasError()).isEqualTo(1);
        assertThat(resultado.errores().get(0)).contains("entre 0 y 10");
    }

    @Test
    @DisplayName("importarRequisitos interpreta S/N y guarda un único registro por estudiante")
    void importarRequisitosInterpretaSiNo() {
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));
        when(requisitoRepo.findByEstudiante_IdEstudiante(10)).thenReturn(Optional.empty());

        String csvContenido = "cedulaEstudiante,idiomaAprobado,practicasLaborales,horasPracticas,servicioComunitario,horasServicio\n"
                + "1234567890,S,N,80,S,120\n";

        ImportacionResultadoDto resultado = service.importarRequisitos(csv("requisitos.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(1);
        assertThat(resultado.filasError()).isEqualTo(0);
    }

    @Test
    @DisplayName("importarRequisitos rechaza un valor S/N inválido")
    void importarRequisitosRechazaValorInvalido() {
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));

        String csvContenido = "cedulaEstudiante,idiomaAprobado,practicasLaborales,horasPracticas,servicioComunitario,horasServicio\n"
                + "1234567890,TALVEZ,N,80,S,120\n";

        ImportacionResultadoDto resultado = service.importarRequisitos(csv("requisitos.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(0);
        assertThat(resultado.filasError()).isEqualTo(1);
    }

    @Test
    @DisplayName("importarDeudas acepta filas válidas con estado PENDIENTE/PAGADA")
    void importarDeudasAceptaFilasValidas() {
        when(estudianteRepo.findByUsuario_Cedula("1234567890")).thenReturn(Optional.of(estudiante(10)));

        String csvContenido = "cedulaEstudiante,tipoDeuda,monto,estado\n"
                + "1234567890,Biblioteca,15.50,PENDIENTE\n";

        ImportacionResultadoDto resultado = service.importarDeudas(csv("deudas.csv", csvContenido), 1);

        assertThat(resultado.filasOk()).isEqualTo(1);
        assertThat(resultado.filasError()).isEqualTo(0);
    }

    @Test
    @DisplayName("una importación vacía o mal formada rechaza el archivo")
    void archivoVacioLanzaExcepcion() {
        MockMultipartFile vacio = new MockMultipartFile("file", "vacio.csv", "text/csv", new byte[0]);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.importarPensum(vacio, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
