import { ChangeDetectionStrategy, Component, inject, model, output, signal } from '@angular/core';
import { Observable, of, switchMap } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TagService } from '../../../../core/services/tag.service';
import { ConfirmService } from '../../../../core/services/confirm.service';
import { TagResponse } from '../../../../core/models/tag.models';

/**
 * Panel plegable para crear tags y, al editarlos, renombrarlos, cambiarles el color o borrarlos (RF-08).
 * tags es bidireccional: [(tags)]; updated y removed avisan para que las preguntas lo reflejen.
 */
@Component({
  selector: 'app-tag-manager',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './tag-manager.component.html',
  styleUrl: './tag-manager.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TagManagerComponent {
  private fb = inject(FormBuilder);
  private tagSvc = inject(TagService);
  private confirmSvc = inject(ConfirmService);

  tags = model.required<TagResponse[]>();
  /** Tag renombrado o con otro color. */
  updated = output<TagResponse>();
  /** Id del tag borrado. */
  removed = output<number>();

  showForm = signal(false);
  /** El tag que se está editando; null = creando uno nuevo. */
  editing = signal<TagResponse | null>(null);
  saving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    color: ['#C62828', Validators.required]
  });

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', color: '#C62828' });
    this.error.set(null);
    this.showForm.set(true);
  }

  openEdit(tag: TagResponse): void {
    this.editing.set(tag);
    this.form.setValue({ name: tag.name, color: tag.color });
    this.error.set(null);
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const { name, color } = this.form.getRawValue();
    const editing = this.editing();
    if (!editing) {
      this.run(this.tagSvc.create({ name: name.trim(), color }), tag => this.tags.update(list => [...list, tag]));
      return;
    }
    // Solo lo que ha cambiado: primero el nombre y después el color
    const renamed = name.trim() !== editing.name;
    const recolored = color.toLowerCase() !== editing.color.toLowerCase();
    if (!renamed && !recolored) {
      this.cancel();
      return;
    }
    let action$: Observable<TagResponse> = of(editing);
    if (renamed) action$ = action$.pipe(switchMap(() => this.tagSvc.rename(editing.id, { newName: name.trim() })));
    if (recolored) action$ = action$.pipe(switchMap(() => this.tagSvc.changeColor(editing.id, color)));
    this.run(action$, tag => {
      this.tags.update(list => list.map(t => t.id === tag.id ? tag : t));
      this.updated.emit(tag);
    });
  }

  /** Borra el tag que se está editando: se quita de sus preguntas, que no se borran (RF-08). */
  remove(): void {
    const tag = this.editing();
    if (!tag || this.saving()) return;
    this.confirmSvc.ask({
      title: 'Borrar tag', confirmLabel: 'Borrar',
      message: `¿Borrar el tag «${tag.name}»? Se quitará de las preguntas que lo tienen; las preguntas no se borran.`
    }).subscribe(ok => {
      if (!ok) return;
      this.run(this.tagSvc.delete(tag.id), () => {
        this.tags.update(list => list.filter(t => t.id !== tag.id));
        this.removed.emit(tag.id);
      });
    });
  }

  private run<T>(action$: Observable<T>, onSuccess: (value: T) => void): void {
    this.saving.set(true);
    this.error.set(null);
    action$.subscribe({
      next: value => {
        this.saving.set(false);
        onSuccess(value);
        this.cancel();
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo guardar el tag');
      }
    });
  }
}
