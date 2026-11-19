package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.IncorporacionDtos.*;
import com.erwin.backend.entities.ActaGrado;
import com.erwin.backend.entities.TituloProfesional;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.enums.EstadoActaGrado;
import com.erwin.backend.enums.EstadoTituloProfesional;
import com.erwin.backend.repository.ActaGradoRepository;
import com.erwin.backend.repository.TituloProfesionalRepository;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Transactional
public class TituloProfesionalService {

    private final TituloProfesionalRepository tituloRepo;
    private final ActaGradoRepository actaGradoRepo;
    private final ActaGradoService actaGradoService;
    private final AuthorizationService authorizationService;

    public TituloProfesionalService(TituloProfesionalRepository tituloRepo,
                                     ActaGradoRepository actaGradoRepo,
                                     ActaGradoService actaGradoService,
                                     AuthorizationService authorizationService) {
        this.tituloRepo = tituloRepo;
        this.actaGradoRepo = actaGradoRepo;
        this.actaGradoService = actaGradoService;
        this.authorizationService = authorizationService;
    }

    @Auditable(entidad = "TituloProfesional", accion = "EMITIR")
    public TituloProfesionalResponse emitir(Integer idActaGrado, EmitirTituloRequest req) {
        ActaGrado acta = actaGradoRepo.findById(idActaGrado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTA_NO_ENCONTRADA"));

        if (acta.getEstado() != EstadoActaGrado.FIRMADA && acta.getEstado() != EstadoActaGrado.REGISTRADA_SENESCYT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El acta debe estar FIRMADA (Rector, Decano y Secretario General) antes de emitir el título");
        }
        if (req.numeroTitulo() == null || req.numeroTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de título es obligatorio");
        }

        TituloProfesional titulo = tituloRepo.findByActaGrado_IdActa(idActaGrado).orElse(new TituloProfesional());
        titulo.setEstudiante(acta.getEstudiante());
        titulo.setActaGrado(acta);
        titulo.setNumeroTitulo(req.numeroTitulo());
        titulo.setFechaEmision(LocalDate.now());
        titulo.setEstado(EstadoTituloProfesional.EMITIDO);

        return toDto(tituloRepo.save(titulo));
    }

    @Auditable(entidad = "TituloProfesional", accion = "REGISTRAR_SENESCYT")
    public TituloProfesionalResponse registrarSenescyt(Long idTitulo, RegistrarSenescytRequest req) {
        TituloProfesional titulo = tituloRepo.findById(idTitulo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TITULO_NO_ENCONTRADO"));

        if (titulo.getEstado() != EstadoTituloProfesional.EMITIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede registrar en SENESCYT un título en estado EMITIDO");
        }
        if (req.registroSenescyt() == null || req.registroSenescyt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de registro SENESCYT es obligatorio");
        }

        titulo.setRegistroSenescyt(req.registroSenescyt());
        titulo.setFechaRegistroSenescyt(LocalDate.now());
        titulo.setEstado(EstadoTituloProfesional.REGISTRADO);
        TituloProfesional guardado = tituloRepo.save(titulo);

        actaGradoService.marcarRegistradaSenescyt(titulo.getActaGrado());

        return toDto(guardado);
    }

    @Transactional(readOnly = true)
    public TituloProfesionalResponse obtenerPorEstudiante(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return tituloRepo.findByEstudiante_IdEstudiante(idEstudiante)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TITULO_NO_ENCONTRADO"));
    }

    private String nombreCompleto(Usuario u) {
        if (u == null) return "";
        return ((u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "")).trim();
    }

    private TituloProfesionalResponse toDto(TituloProfesional t) {
        return new TituloProfesionalResponse(
                t.getId(),
                t.getEstudiante() != null ? t.getEstudiante().getIdEstudiante() : null,
                t.getEstudiante() != null ? nombreCompleto(t.getEstudiante().getUsuario()) : null,
                t.getActaGrado() != null ? t.getActaGrado().getIdActa() : null,
                t.getNumeroTitulo(),
                t.getFechaEmision(),
                t.getRegistroSenescyt(),
                t.getFechaRegistroSenescyt(),
                t.getEstado().name()
        );
    }
}
