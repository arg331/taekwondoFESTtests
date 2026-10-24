import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TagResponse } from '../../../core/models/tag.models';

/**
 * Filtro por tags del banco y del selector del examen (RF-10, RF-14): una casilla por tag, con su
 * número de preguntas, en una cuadrícula. «Desmarcar tags» va pegado al borde, fuera de ella.
 * Sin ninguno marcado se ven todas las preguntas; con alguno, las que tengan alguno de ellos.
 */
@Component({
  selector: 'app-tag-filter',
  imports: [MatCheckboxModule],
  templateUrl: './tag-filter.component.html',
  styleUrl: './tag-filter.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TagFilterComponent {
  tags = input.required<TagResponse[]>();
  /** Preguntas que tiene cada tag (id → número). */
  counts = input.required<ReadonlyMap<number, number>>();
  selectedIds = model<number[]>([]);

  isSelected(tagId: number): boolean {
    return this.selectedIds().includes(tagId);
  }

  setSelected(tagId: number, selected: boolean): void {
    this.selectedIds.update(ids => selected ? [...ids, tagId] : ids.filter(id => id !== tagId));
  }

  clear(): void {
    this.selectedIds.set([]);
  }
}
