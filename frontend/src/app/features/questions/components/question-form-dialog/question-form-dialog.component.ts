import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { QuestionFormComponent } from '../question-form/question-form.component';
import { QuestionResponse } from '../../../../core/models/question.models';
import { TagResponse } from '../../../../core/models/tag.models';

export interface QuestionFormDialogData {
  tags: TagResponse[];
}

/**
 * El formulario de preguntas dentro de un diálogo, para crear una sin salir de la pantalla actual.
 * Se cierra devolviendo la pregunta guardada, o nada si se cancela.
 */
@Component({
  selector: 'app-question-form-dialog',
  imports: [QuestionFormComponent],
  template: `
    <app-question-form [question]="null" [tags]="data.tags" (saved)="close($event)" (cancelled)="close()" />
  `,
  styles: `:host { display: block; }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionFormDialogComponent {
  private dialogRef = inject<MatDialogRef<QuestionFormDialogComponent, QuestionResponse>>(MatDialogRef);
  readonly data = inject<QuestionFormDialogData>(MAT_DIALOG_DATA);

  close(saved?: QuestionResponse): void {
    this.dialogRef.close(saved);
  }
}
