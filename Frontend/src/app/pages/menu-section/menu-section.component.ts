import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { MenuService } from '../../layout/menu.service';

@Component({
  selector: 'app-menu-section',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './menu-section.component.html',
  styleUrls: ['./menu-section.component.scss'],
})
export class MenuSectionComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly menuService = inject(MenuService);

  private readonly slug = signal(this.route.snapshot.paramMap.get('slug') ?? '');

  readonly section = computed(() => this.menuService.getSectionBySlug(this.slug()));

  constructor() {
    this.route.paramMap.subscribe(params => {
      this.slug.set(params.get('slug') ?? '');
      if (!this.section()) {
        this.router.navigate(['/app/inicio']);
      }
    });
  }
}
