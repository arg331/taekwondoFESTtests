import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EXAM_STATUS_COLOR, EXAM_STATUS_ICON, ExamResponse } from '../../../../core/models/exam.models';

@Component({
  selector: 'app-exam-list',
  imports: [MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './exam-list.component.html',
  styleUrl: './exam-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamListComponent {
  exams = input.required<ExamResponse[]>();
  selectedId = input<number | null>(null);
  selected = output<ExamResponse>();

  readonly statusColor = EXAM_STATUS_COLOR;
  readonly statusIcon = EXAM_STATUS_ICON;
}
