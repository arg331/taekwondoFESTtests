import { ChangeDetectionStrategy, Component, computed, input, model, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import {
  DIFFICULTIES,
  DIFFICULTY_LABEL,
  Difficulty,
  QuestionResponse
} from '../../../../core/models/question.models';

/**
 * Lista filtrable de preguntas del banco con selección múltiple.
 * selectedIds es bidireccional: [(selectedIds)].
 */
@Component({
  selector: 'app-exam-question-picker',
  imports: [MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule],
  templateUrl: './exam-question-picker.component.html',
  styleUrl: './exam-question-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamQuestionPickerComponent {
  questions = input.required<QuestionResponse[]>();
  selectedIds = model.required<number[]>();

  readonly difficulties = DIFFICULTIES;
  readonly diffLabel = DIFFICULTY_LABEL;

  search = signal('');
  difficulty = signal<Difficulty | ''>('');

  filtered = computed(() => {
    const text = this.search().toLowerCase().trim();
    const diff = this.difficulty();
    return this.questions().filter(q =>
      (!text || q.text.toLowerCase().includes(text)) && (!diff || q.difficulty === diff));
  });

  isSelected(id: number): boolean {
    return this.selectedIds().includes(id);
  }

  toggle(id: number): void {
    this.selectedIds.update(ids => ids.includes(id) ? ids.filter(q => q !== id) : [...ids, id]);
  }
}
