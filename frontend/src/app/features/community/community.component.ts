import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, linkedSignal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityService } from '../../core/services/community.service';
import { CommunityExamResponse } from '../../core/models/community.models';
import { PaginatorIntlEs } from '../../shared/paginator-intl-es';
import { PAGE_SIZE, clampPage, pageItems } from '../../shared/utils/pagination';
import { CommunityExamDetailComponent } from './components/community-exam-detail/community-exam-detail.component';

type Filter = 'all' | 'favorites';

const MESSAGE_DURATION_MS = 4000;

/**
 * Exámenes de prueba de otros profesores (RF-30): la lista a la izquierda, con favoritos (RF-31), y el elegido a la
 * derecha con sus preguntas para copiarlas a mi banco (DE-12).
 */
@Component({
  selector: 'app-community',
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    CommunityExamDetailComponent
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginatorIntlEs }],
  templateUrl: './community.component.html',
  styleUrl: './community.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CommunityComponent implements OnInit {
  private communitySvc = inject(CommunityService);
  private snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly pageSize = PAGE_SIZE;

  loading = signal(true);
  exams = signal<CommunityExamResponse[]>([]);
  filter = signal<Filter>('all');
  query = signal('');
  selectedId = signal<number | null>(null);

  favoriteCount = computed(() => this.exams().filter(e => e.favorite).length);
  visible = computed(() => {
    const q = normalize(this.query());
    return this.exams()
      .filter(e => this.filter() === 'all' || e.favorite)
      .filter(e => !q || normalize(e.title).includes(q) || normalize(e.ownerName).includes(q));
  });
  /** Vuelve a la primera página al buscar o cambiar de filtro. */
  page = linkedSignal({ source: () => [this.filter(), this.query()], computation: () => 0 });
  pageExams = computed(() => pageItems(this.visible(), clampPage(this.page(), this.visible().length)));
  selected = computed(() => this.exams().find(e => e.id === this.selectedId()) ?? null);

  ngOnInit(): void {
    this.communitySvc.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: exams => { this.exams.set(exams); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
  }

  toggleFavorite(exam: CommunityExamResponse): void {
    const favorite = !exam.favorite;
    const request = favorite ? this.communitySvc.favorite(exam.id) : this.communitySvc.unfavorite(exam.id);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.exams.update(list => list.map(e => (e.id === exam.id ? { ...e, favorite } : e))),
      error: err => this.snackBar.open(err?.error?.message ?? 'No se pudo cambiar el favorito', undefined,
        { duration: MESSAGE_DURATION_MS })
    });
  }
}

/** Sin mayúsculas ni tildes, para buscar. */
function normalize(text: string): string {
  return text.trim().toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '');
}
