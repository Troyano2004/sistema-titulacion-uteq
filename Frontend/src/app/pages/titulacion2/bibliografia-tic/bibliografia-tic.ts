import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import {
  BibliografiaTicService,
  ReferenciaBibliografica,
  BibliografiaFormateada
} from '../../../services/bibliografia-tic.service';
import { DocumentoTicService } from '../../../services/documento-tic.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-bibliografia-tic',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './bibliografia-tic.html',
  styleUrl: './bibliografia-tic.scss'
})
export class BibliografiaTicComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  idDocumento = signal<number | null>(null);
  referencias = signal<ReferenciaBibliografica[]>([]);
  formateada = signal<BibliografiaFormateada | null>(null);

  mostrarFormulario = signal(false);
  editandoId: number | null = null;
  form: FormGroup;

  constructor(
    private bibliografiaApi: BibliografiaTicService,
    private documentoApi: DocumentoTicService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      autores: ['', Validators.required],
      titulo: ['', Validators.required],
      anioPublicacion: [new Date().getFullYear(), Validators.required],
      fuente: [''],
      tipo: ['ARTICULO', Validators.required]
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    const idEstudiante = getSessionEntityId(user, 'estudiante');
    if (!idEstudiante) return;

    this.documentoApi.obtenerPorEstudiante(idEstudiante).subscribe({
      next: doc => { this.idDocumento.set(doc.id); this.cargar(); },
      error: () => this.error.set('Primero debes crear tu documento de titulación')
    });
  }

  private cargar(): void {
    const id = this.idDocumento();
    if (!id) return;
    this.loading.set(true);
    this.bibliografiaApi.listar(id).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => this.referencias.set(data),
      error: () => this.error.set('Error al cargar la bibliografía')
    });
    this.cargarFormateada();
  }

  private cargarFormateada(): void {
    const id = this.idDocumento();
    if (!id) return;
    this.bibliografiaApi.formateada(id).subscribe({
      next: data => this.formateada.set(data),
      error: () => {}
    });
  }

  nuevaReferencia(): void {
    this.editandoId = null;
    this.form.reset({ anioPublicacion: new Date().getFullYear(), tipo: 'ARTICULO' });
    this.mostrarFormulario.set(true);
  }

  editar(r: ReferenciaBibliografica): void {
    this.editandoId = r.id;
    this.form.reset({ autores: r.autores, titulo: r.titulo, anioPublicacion: r.anioPublicacion, fuente: r.fuente, tipo: r.tipo });
    this.mostrarFormulario.set(true);
  }

  cancelar(): void {
    this.mostrarFormulario.set(false);
  }

  guardar(): void {
    const id = this.idDocumento();
    if (!id || this.form.invalid) { this.form.markAllAsTouched(); return; }

    const req = this.form.value;
    const obs = this.editandoId
      ? this.bibliografiaApi.actualizar(id, this.editandoId, req)
      : this.bibliografiaApi.crear(id, req);

    this.loading.set(true);
    obs.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.ok.set(this.editandoId ? 'Referencia actualizada' : 'Referencia agregada');
        this.mostrarFormulario.set(false);
        this.cargar();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al guardar la referencia')
    });
  }

  eliminar(r: ReferenciaBibliografica): void {
    const id = this.idDocumento();
    if (!id || !confirm(`¿Eliminar la referencia "${r.titulo}"?`)) return;
    this.bibliografiaApi.eliminar(id, r.id).subscribe({
      next: () => { this.ok.set('Referencia eliminada'); this.cargar(); },
      error: () => this.error.set('Error al eliminar la referencia')
    });
  }
}
