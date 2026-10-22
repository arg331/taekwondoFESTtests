import { Injectable } from '@angular/core';
import { toDataURL } from 'qrcode';
import { QR_DEFAULT_COLOR } from './qr-color.service';

/** Lado del QR en píxeles: el del panel se pinta a la mitad para que se vea nítido. */
export const QR_PANEL_SIZE_PX = 400;
export const QR_PROJECTION_SIZE_PX = 1024;

/**
 * Genera códigos QR en el navegador, sin enviar el enlace del examen a ningún servicio externo.
 * Aísla la librería: si se cambia, solo se toca este fichero.
 */
@Injectable({ providedIn: 'root' })
export class QrCodeService {

  /** PNG como data URL, listo para un <img> o para descargarlo. Módulos del color pedido sobre blanco. */
  toDataUrl(text: string, sizePx: number, color = QR_DEFAULT_COLOR): Promise<string> {
    return toDataURL(text, {
      width: sizePx,
      margin: 2,
      errorCorrectionLevel: 'M',
      color: { dark: color, light: '#ffffff' }
    });
  }
}
