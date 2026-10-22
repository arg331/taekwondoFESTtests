import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { normalizeExamCode } from '../../utils/exam-code';

/**
 * Entrada manual del código de un examen, para quien no puede escanear el QR.
 * Acepta el código en cualquier forma razonable (minúsculas, sin EXM-, el enlace entero).
 */
@Component({
  selector: 'app-exam-code-entry',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <form class="code-form" [formGroup]="form" (ngSubmit)="go()">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Código del examen</mat-label>
        <input matInput formControlName="code" placeholder="EXM-1A2B3C4D" autocomplete="off" />
        <mat-icon matPrefix>qr_code</mat-icon>
      </mat-form-field>
      <button mat-flat-button color="primary" type="submit">Ir al examen</button>
    </form>
    @if (invalid()) {
      <p class="code-error">Ese código no es válido. Tiene esta forma: EXM-1A2B3C4D.</p>
    }
  `,
  styles: `
    .code-form { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .code-form mat-form-field { flex: 1; min-width: 200px; }
    .code-error { color: #f44336; font-size: 0.85rem; margin: 4px 0 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamCodeEntryComponent {
  private router = inject(Router);

  form = new FormGroup({ code: new FormControl('', { nonNullable: true }) });
  invalid = signal(false);

  go(): void {
    const examCode = normalizeExamCode(this.form.controls.code.value);
    this.invalid.set(examCode === null);
    if (examCode) {
      this.router.navigate(['/exam', examCode]);
    }
  }
}
