import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

/** Lo que pregunta el diálogo de confirmación. */
export interface ConfirmOptions {
  title: string;
  message: string;
  /** El botón que hace la acción, p. ej. «Borrar». */
  confirmLabel: string;
  cancelLabel?: string;
}

/**
 * Confirmación con el aspecto de la app, en lugar del confirm() del navegador. El foco empieza en
 * «Cancelar»: pulsar Intro sin mirar no borra nada.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content><p>{{ data.message }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false" cdkFocusInitial>{{ data.cancelLabel ?? 'Cancelar' }}</button>
      <button mat-flat-button color="primary" [mat-dialog-close]="true">{{ data.confirmLabel }}</button>
    </mat-dialog-actions>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
}
