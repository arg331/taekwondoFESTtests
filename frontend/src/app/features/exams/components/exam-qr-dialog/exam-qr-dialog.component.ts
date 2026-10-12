import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, resource } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { QR_PROJECTION_SIZE_PX, QrCodeService } from '../../../../core/services/qr-code.service';
import { QR_COLOR_PRESETS, QrColorService, isLowContrast } from '../../../../core/services/qr-color.service';

export interface ExamQrDialogData {
  title: string;
  code: string;
  url: string;
}

/**
 * QR grande para proyectar en el aula, con el título, el código y el enlace
 * para quien no pueda escanearlo. Se puede pasar a pantalla completa y descargar. Cada examen
 * tiene su color, para que en el aula se distinga qué QR escanear.
 */
@Component({
  selector: 'app-exam-qr-dialog',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './exam-qr-dialog.component.html',
  styleUrl: './exam-qr-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamQrDialogComponent {
  private dialogRef = inject(MatDialogRef<ExamQrDialogComponent>);
  private host = inject(ElementRef<HTMLElement>);
  private document = inject(DOCUMENT);
  private qrCode = inject(QrCodeService);
  private qrColor = inject(QrColorService);

  readonly data = inject<ExamQrDialogData>(MAT_DIALOG_DATA);
  readonly colorPresets = QR_COLOR_PRESETS;
  readonly color = computed(() => this.qrColor.colorOf(this.data.code));
  readonly lowContrast = computed(() => isLowContrast(this.color()));
  readonly qrImage = resource({
    params: () => this.color(),
    loader: ({ params: color }) => this.qrCode.toDataUrl(this.data.url, QR_PROJECTION_SIZE_PX, color)
  });

  constructor() {
    // Si se cierra estando a pantalla completa, el navegador no debe quedarse así
    inject(DestroyRef).onDestroy(() => {
      if (this.document.fullscreenElement) {
        this.document.exitFullscreen();
      }
    });
  }

  fullscreen(): void {
    this.host.nativeElement.requestFullscreen?.();
  }

  setColor(color: string): void {
    this.qrColor.setColor(this.data.code, color);
  }

  close(): void {
    this.dialogRef.close();
  }
}
