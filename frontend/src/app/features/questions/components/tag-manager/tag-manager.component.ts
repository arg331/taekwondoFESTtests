import { ChangeDetectionStrategy, Component, inject, model, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TagService } from '../../../../core/services/tag.service';
import { TagResponse } from '../../../../core/models/tag.models';

/**
 * Panel plegable para crear y renombrar tags. tags es bidireccional: [(tags)].
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

  tags = model.required<TagResponse[]>();

  showForm = signal(false);
  editingId = signal<number | null>(null);
  saving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    color: ['#C62828', Validators.required]
  });

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', color: '#C62828' });
    this.error.set(null);
    this.showForm.set(true);
  }

  openEdit(tag: TagResponse): void {
    this.editingId.set(tag.id);
    this.form.setValue({ name: tag.name, color: tag.color });
    this.error.set(null);
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const { name, color } = this.form.getRawValue();
    const id = this.editingId();
    this.saving.set(true);
    this.error.set(null);

    (id ? this.tagSvc.rename(id, { newName: name.trim() }) : this.tagSvc.create({ name: name.trim(), color }))
      .subscribe({
        next: tag => {
          this.tags.update(list => id ? list.map(t => t.id === tag.id ? tag : t) : [...list, tag]);
          this.saving.set(false);
          this.cancel();
        },
        error: err => {
          this.saving.set(false);
          this.error.set(err?.error?.message ?? 'No se pudo guardar el tag');
        }
      });
  }
}
