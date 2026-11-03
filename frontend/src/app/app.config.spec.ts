import { TestBed } from '@angular/core/testing';
import { OVERLAY_DEFAULT_CONFIG } from '@angular/cdk/overlay';
import { appConfig } from './app.config';

describe('appConfig', () => {
  it('las capas de la CDK no usan popover: un desplegable dentro de un diálogo se puede elegir', () => {
    TestBed.configureTestingModule({ providers: appConfig.providers });

    expect(TestBed.inject(OVERLAY_DEFAULT_CONFIG, null)?.usePopover).toBe(false);
  });
});
