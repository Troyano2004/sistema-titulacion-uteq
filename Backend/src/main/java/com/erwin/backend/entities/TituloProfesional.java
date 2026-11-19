package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoTituloProfesional;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "titulo_profesional")
public class TituloProfesional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_titulo")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_acta", nullable = false, unique = true)
    private ActaGrado actaGrado;

    @Column(name = "numero_titulo", length = 50)
    private String numeroTitulo;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "registro_senescyt", length = 100)
    private String registroSenescyt;

    @Column(name = "fecha_registro_senescyt")
    private LocalDate fechaRegistroSenescyt;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoTituloProfesional estado = EstadoTituloProfesional.PENDIENTE;
}
