import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { ConfirmDialogComponent, ConfirmOptions } from '../../shared/components/confirm-dialog/confirm-dialog.component';

/** Pide confirmación antes de una acción que no se deshace fácil. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private dialog = inject(MatDialog);

  /** true solo si se pulsa el botón de confirmar; cancelar o cerrar el diálogo es false. */
  ask(options: ConfirmOptions): Observable<boolean> {
    return this.dialog.open<ConfirmDialogComponent, ConfirmOptions, boolean>(ConfirmDialogComponent, {
      data: options, width: '420px', maxWidth: '95vw', role: 'alertdialog', autoFocus: 'first-tabbable'
    }).afterClosed().pipe(map(answer => answer === true));
  }
}
