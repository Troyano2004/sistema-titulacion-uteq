
import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { filter, Subscription } from 'rxjs';
import { getSessionUser, getUserRoles } from '../../services/session';
import { MenuService } from '../menu.service';
import { AppRole, SearchResult } from '../menu.model';

const ROLE_LABELS: Record<AppRole, string> = {
  ADMIN: 'Administrador',
  DOCENTE: 'Docente',
  DOCENTE_TITULADO: 'Docente Titulado',
  ESTUDIANTE: 'Estudiante',
  COORDINADOR: 'Coordinador',
  DECANO: 'Decano',
  SECRETARIO_ABOGADO: 'Secretario/Abogado',
  DIRECCION_ACADEMICA: 'Dirección Académica',
  GESTOR_USUARIOS: 'Gestor de Usuarios',
  ROL_REPORT: 'Reportes',
};

export interface BreadcrumbSegment {
  label: string;
  /** null = paso actual, no clicable */
  path: string | null;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit, OnDestroy {
  currentTitle = 'Inicio';
  breadcrumbSegments: BreadcrumbSegment[] = [{ label: 'Inicio', path: null }];
  /** Ruta a la que va el botón "← Volver"; null en /app/inicio (ya es el home). */
  volverPath: string | null = null;
  userName = 'Usuario';
  userInitials = 'US';
  isDarkMode = false;

  searchQuery = '';
  searchFocused = false;
  searchResults: SearchResult[] = [];

  /** Selector de rol activo (topbar): filtra el menú cuando el usuario tiene varios roles. */
  roleMenuOpen = false;
  roleLabels = ROLE_LABELS;

  private readonly subscriptions = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly menuService: MenuService,
    private readonly elementRef: ElementRef<HTMLElement>,
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.loadTheme();
    this.updateTitles();

    const sub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateTitles());

    this.subscriptions.add(sub);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  toggleTheme(): void {
    this.isDarkMode = !this.isDarkMode;
    document.body.classList.toggle('dark-mode', this.isDarkMode);
    localStorage.setItem('theme', this.isDarkMode ? 'dark' : 'light');
  }

  logout(): void {
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
    localStorage.removeItem('activeRoleFilter');
    this.router.navigate(['/login']);
  }

  updateSearchResults(): void {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      this.searchResults = [];
      return;
    }

    const seen = new Set<string>();
    const results: SearchResult[] = [];

    for (const section of this.menuService.getAllSections()) {
      for (const item of section.items) {
        if (seen.has(item.path)) continue;

        if (item.label.toLowerCase().includes(q) || section.title.toLowerCase().includes(q)) {
          seen.add(item.path);
          results.push({
            label: item.label,
            section: section.title,
            icon: section.icon,
            path: item.path,
          });
        }

        if (results.length >= 7) break;
      }

      if (results.length >= 7) break;
    }

    this.searchResults = results;
  }

  navigateToFirst(): void {
    if (this.searchResults.length > 0) {
      this.router.navigate([this.searchResults[0].path]);
      this.clearSearch();
    }
  }

  onResultClick(): void {
    this.clearSearch();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.searchFocused = false;
  }

  onSearchBlur(): void {
    setTimeout(() => {
      this.searchFocused = false;
      this.searchResults = [];
    }, 160);
  }

  private loadTheme(): void {
    const saved = localStorage.getItem('theme');
    if (saved === 'dark') {
      this.isDarkMode = true;
      document.body.classList.add('dark-mode');
    }
  }

  private loadUserData(): void {
    const user = getSessionUser();
    if (!user) return;

    const fullName = [user['nombres'], user['apellidos']]
      .map(v => String(v ?? '').trim())
      .filter(v => v.length > 0)
      .join(' ');

    const backupName = String(user['username'] || user['usuarioLogin'] || 'Usuario');
    this.userName = fullName || backupName;
    this.userInitials = this.buildInitials(this.userName);
  }

  /** Rol(es) que se muestran junto al nombre: el filtrado si hay uno activo, o todos. */
  get userRole(): string {
    const activo = this.menuService.activeRole();
    if (activo) return this.roleLabels[activo] ?? activo;

    const roles = getUserRoles();
    if (roles.length > 0) {
      return roles.map(r => r.replace('ROLE_', '')).join(' | ');
    }
    const user = getSessionUser();
    return String(user?.['rol'] ?? '').replace('ROLE_', '').trim() || 'Sistema';
  }

  /** Roles reales del usuario, para el selector desplegable. */
  get misRoles(): AppRole[] {
    return this.menuService.misRoles();
  }

  /** true si el usuario tiene más de un rol (el selector solo tiene sentido ahí). */
  get tieneVariosRoles(): boolean {
    return this.misRoles.length > 1;
  }

  get activeRole(): AppRole | null {
    return this.menuService.activeRole();
  }

  roleLabel(role: AppRole): string {
    return this.roleLabels[role] ?? role;
  }

  toggleRoleMenu(): void {
    if (!this.tieneVariosRoles) return;
    this.roleMenuOpen = !this.roleMenuOpen;
  }

  selectRole(role: AppRole | null): void {
    this.menuService.setActiveRole(role);
    this.roleMenuOpen = false;
    // Si la ruta actual no pertenece al rol elegido, evita dejar al usuario
    // en una página que ya no ve en su menú.
    const url = this.router.url.split('?')[0];
    if (url !== '/app/inicio' && !this.menuService.findBreadcrumb(url)) {
      this.router.navigate(['/app/inicio']);
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.roleMenuOpen) return;
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.roleMenuOpen = false;
      return;
    }
    const target = event.target as HTMLElement;
    if (!target.closest('.avatar') && !target.closest('.role-menu')) {
      this.roleMenuOpen = false;
    }
  }

  private buildInitials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return 'US';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  private updateTitles(): void {
    const url = this.router.url.split('?')[0];
    const inicio: BreadcrumbSegment = { label: 'Inicio', path: '/app/inicio' };

    if (url === '/app/inicio') {
      this.currentTitle = 'Inicio';
      this.breadcrumbSegments = [{ label: 'Inicio', path: null }];
      this.volverPath = null;
      return;
    }

    const menuSlugMatch = url.match(/^\/app\/menu\/([^/]+)$/);
    if (menuSlugMatch) {
      const section = this.menuService.getSectionBySlug(menuSlugMatch[1]);
      const title = section?.title ?? 'Menú';
      this.currentTitle = title;
      this.breadcrumbSegments = [inicio, { label: title, path: null }];
      this.volverPath = '/app/inicio';
      return;
    }

    const found = this.menuService.findBreadcrumb(url);
    if (found) {
      const sectionPath = '/app/menu/' + this.menuService.slugify(found.section.title);
      this.currentTitle = found.item.label;
      this.breadcrumbSegments = [
        inicio,
        { label: found.section.title, path: sectionPath },
        { label: found.item.label, path: null },
      ];
      this.volverPath = sectionPath;
      return;
    }

    // Ruta que no está listada como item de menú (p.ej. una página de detalle
    // a la que se llega desde otra página, no desde el menú de cajas):
    // no sabemos su "sección", así que el botón Volver manda a Inicio.
    const title = this.getDeepestTitle(this.route) ?? 'Página';
    this.currentTitle = title;
    this.breadcrumbSegments = [inicio, { label: title, path: null }];
    this.volverPath = '/app/inicio';
  }

  private getDeepestTitle(route: ActivatedRoute): string | undefined {
    let current = route;
    while (current.firstChild) current = current.firstChild;
    return current.snapshot.data['title'] as string | undefined;
  }
}
