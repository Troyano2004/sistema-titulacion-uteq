import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import {
  IncorporacionService,
  PendienteIncorporacion,
  ActaGradoIncorporacion,
  TituloProfesional,
  TipoFirma
} from '../../../services/incorporacion.service';
import { CatalogosBasicosService, Facultad } from '../../../services/catalogos-basicos.service';

@Component({
  selector: 'app-panel-incorporacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './panel-incorporacion.html',
  styleUrl: './panel-incorporacion.scss'
})
export class PanelIncorporacionComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  facultades = signal<Facultad[]>([]);
  filtroFacultad: number | null = null;

  pendientes = signal<PendienteIncorporacion[]>([]);
  seleccionado = signal<PendienteIncorporacion | null>(null);
  actaDetalle = signal<ActaGradoIncorporacion | null>(null);
  titulo = signal<TituloProfesional | null>(null);

  numeroTituloNuevo = '';
  registroSenescytNuevo = '';
  fechaCeremoniaNueva = '';

  constructor(private incorporacionApi: IncorporacionService, private catalogosApi: CatalogosBasicosService) {}

  ngOnInit(): void {
    this.catalogosApi.listarFacultades().subscribe({ next: d => this.facultades.set(d), error: () => {} });
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.incorporacionApi.pendientes(this.filtroFacultad ?? undefined)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.pendientes.set(data),
        error: () => this.error.set('No se pudieron cargar los pendientes de incorporación')
      });
  }

  seleccionar(p: PendienteIncorporacion): void {
    this.seleccionado.set(p);
    this.titulo.set(null);
    this.incorporacionApi.obtenerPorEstudiante(p.idEstudiante).subscribe({
      next: acta => {
        this.actaDetalle.set(acta);
        if (acta.estado !== 'GENERADA') {
          this.incorporacionApi.obtenerTituloPorEstudiante(p.idEstudiante).subscribe({
            next: t => this.titulo.set(t),
            error: () => this.titulo.set(null)
          });
        }
      },
      error: () => this.error.set('No se pudo cargar el detalle del acta')
    });
  }

  firmar(tipo: TipoFirma): void {
    const acta = this.actaDetalle();
    if (!acta) return;
    this.incorporacionApi.firmar(acta.idActa, tipo).subscribe({
      next: actualizada => { this.actaDetalle.set(actualizada); this.ok.set('Firma registrada'); this.cargar(); },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al registrar la firma')
    });
  }

  programarCeremonia(): void {
    const acta = this.actaDetalle();
    if (!acta || !this.fechaCeremoniaNueva) return;
    this.incorporacionApi.programarCeremonia(acta.idActa, this.fechaCeremoniaNueva).subscribe({
      next: actualizada => { this.actaDetalle.set(actualizada); this.ok.set('Ceremonia programada'); },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al programar la ceremonia')
    });
  }

  emitirTitulo(): void {
    const acta = this.actaDetalle();
    if (!acta || !this.numeroTituloNuevo.trim()) return;
    this.incorporacionApi.emitirTitulo(acta.idActa, this.numeroTituloNuevo.trim()).subscribe({
      next: t => { this.titulo.set(t); this.numeroTituloNuevo = ''; this.ok.set('Título emitido'); },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al emitir el título')
    });
  }

  registrarSenescyt(): void {
    const t = this.titulo();
    if (!t || !this.registroSenescytNuevo.trim()) return;
    this.incorporacionApi.registrarSenescyt(t.id, this.registroSenescytNuevo.trim()).subscribe({
      next: actualizado => {
        this.titulo.set(actualizado);
        this.registroSenescytNuevo = '';
        this.ok.set('Registro SENESCYT guardado');
        this.cargar();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al registrar SENESCYT')
    });
  }
}
