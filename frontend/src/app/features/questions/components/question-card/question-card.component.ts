import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL, QuestionResponse } from '../../../../core/models/question.models';

@Component({
  selector: 'app-question-card',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './question-card.component.html',
  styleUrl: './question-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionCardComponent {
  question = input.required<QuestionResponse>();
  edit = output<void>();
  remove = output<void>();

  readonly diffLabel = DIFFICULTY_LABEL;
  readonly diffColor = DIFFICULTY_COLOR;
}
