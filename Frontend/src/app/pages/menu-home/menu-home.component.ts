import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { MenuService } from '../../layout/menu.service';
import { MenuSection } from '../../layout/menu.model';

@Component({
  selector: 'app-menu-home',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './menu-home.component.html',
  styleUrls: ['./menu-home.component.scss'],
})
export class MenuHomeComponent {
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);

  readonly sections = this.menuService.visibleSections;

  abrir(section: MenuSection): void {
    this.router.navigate(['/app/menu', this.menuService.slugify(section.title)]);
  }
}
