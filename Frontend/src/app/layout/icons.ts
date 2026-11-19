import {
  LayoutDashboard, GraduationCap, BookOpen, FilePenLine, Users, FileText,
  ClipboardCheck, ClipboardList, CircleQuestionMark, FolderOpen, Scale,
  ChartColumn, CalendarDays, Archive, Award, Compass, Landmark, Gavel, Settings,
  Moon, Sun, Bell, Search, X, ArrowLeft, ChevronRight, ChevronDown, Check,
} from 'lucide-angular';

/**
 * Set único de íconos SVG (Lucide) usados en toda la app — el menú de
 * secciones y la topbar. Se registra una sola vez en app.config.ts vía
 * LucideAngularModule.pick(APP_ICONS); cada componente que los use solo
 * necesita importar LucideAngularModule y referenciar el nombre en
 * kebab-case (p.ej. "layout-dashboard") desde el atributo [name].
 */
export const APP_ICONS = {
  LayoutDashboard, GraduationCap, BookOpen, FilePenLine, Users, FileText,
  ClipboardCheck, ClipboardList, CircleQuestionMark, FolderOpen, Scale,
  ChartColumn, CalendarDays, Archive, Award, Compass, Landmark, Gavel, Settings,
  Moon, Sun, Bell, Search, X, ArrowLeft, ChevronRight, ChevronDown, Check,
};
