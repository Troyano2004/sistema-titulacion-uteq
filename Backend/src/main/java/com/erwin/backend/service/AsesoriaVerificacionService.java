package com.erwin.backend.service;

import com.erwin.backend.dtos.VerificacionMensualResponse;
import com.erwin.backend.entities.AsesoriaDirector;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.repository.AsesoriaDirectorRepository;
import com.erwin.backend.repository.ProyectoTitulacionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * Módulo 8: verifica el mínimo de 2 reuniones (asesorías) mensuales que exige
 * el reglamento durante el desarrollo del TIC.
 */
@Service
@Transactional(readOnly = true)
public class AsesoriaVerificacionService {

    private static final int MIN_REUNIONES_MENSUALES = 2;

    private final ProyectoTitulacionRepository proyectoRepo;
    private final AsesoriaDirectorRepository asesoriaRepo;

    public AsesoriaVerificacionService(ProyectoTitulacionRepository proyectoRepo,
                                        AsesoriaDirectorRepository asesoriaRepo) {
        this.proyectoRepo = proyectoRepo;
        this.asesoriaRepo = asesoriaRepo;
    }

    public VerificacionMensualResponse verificarCumplimiento(Integer estudianteId, Integer mes, Integer anio) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        if (anio == null) {
            throw new IllegalArgumentException("El año es obligatorio");
        }

        ProyectoTitulacion proyecto = proyectoRepo
                .findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El estudiante no tiene un proyecto de titulación registrado: " + estudianteId));

        YearMonth mesConsultado = YearMonth.of(anio, mes);

        long totalReuniones = asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(proyecto.getIdProyecto())
                .stream()
                .map(AsesoriaDirector::getFecha)
                .filter(f -> f != null && YearMonth.from(f).equals(mesConsultado))
                .count();

        return new VerificacionMensualResponse(estudianteId, mes, anio, totalReuniones, totalReuniones >= MIN_REUNIONES_MENSUALES);
    }
}
